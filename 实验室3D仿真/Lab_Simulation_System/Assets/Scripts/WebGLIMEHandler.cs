using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using UnityEngine;
using UnityEngine.EventSystems;
using TMPro;

/// <summary>
/// WebGL 中文输入法（IME）支持
/// 在 WebGL 构建中，TMP_InputField 无法正常使用系统输入法。
/// 此脚本通过创建 HTML overlay input 元素来解决中文输入问题。
/// 
/// 用法：将此脚本挂到场景中任意 GameObject 上即可自动生效。
///      或命名为 "IMEHandler" 以匹配 jslib 默认 SendMessage 目标。
/// </summary>
public class WebGLIMEHandler : MonoBehaviour
{
    private static WebGLIMEHandler instance;

    private readonly Dictionary<TMP_InputField, string> fieldMap = new Dictionary<TMP_InputField, string>();
    private TMP_InputField currentField;
    private TMP_InputField imeVisibleField;
    private Canvas canvasRoot;
    private float nextFieldScanTime;
    private GameObject lastSelectedObject;

    /// <summary>IME 输入框当前是否可见</summary>
    public static bool IsVisible
    {
        get
        {
#if UNITY_WEBGL && !UNITY_EDITOR
            return SmartLabIME_IsVisible() != 0;
#else
            return false;
#endif
        }
    }

#if UNITY_WEBGL && !UNITY_EDITOR
    [DllImport("__Internal")]
    private static extern void SmartLabIME_Create();

    [DllImport("__Internal")]
    private static extern void SmartLabIME_Show(int x, int y, int w, int h, string text, string target, string fieldName);

    [DllImport("__Internal")]
    private static extern void SmartLabIME_Hide();

    [DllImport("__Internal")]
    private static extern int SmartLabIME_IsVisible();

    [DllImport("__Internal")]
    private static extern int SmartLabIME_HasText();

    [DllImport("__Internal")]
    private static extern System.IntPtr SmartLabIME_GetText();
#endif

    [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
    private static void AutoCreate()
    {
        if (instance != null) return;

        GameObject go = new GameObject("IMEHandler");
        go.AddComponent<WebGLIMEHandler>();
        DontDestroyOnLoad(go);
    }

    private void Awake()
    {
        if (instance != null && instance != this)
        {
            Destroy(gameObject);
            return;
        }

        instance = this;
        // 确保 GameObject 名称匹配 jslib 默认目标
        if (gameObject.name != "IMEHandler")
            gameObject.name = "IMEHandler";

#if UNITY_WEBGL && !UNITY_EDITOR
        SmartLabIME_Create();
#endif
    }

    private void Start()
    {
        canvasRoot = GetComponentInParent<Canvas>();
        if (canvasRoot == null)
            canvasRoot = FindObjectOfType<Canvas>();

        // 延迟扫描：等待所有 UI 完全初始化
        Invoke(nameof(ScanInputFields), 0.5f);
    }

    private void OnDestroy()
    {
        if (instance == this) instance = null;
    }

    /// <summary>
    /// 扫描场景中所有 TMP_InputField 并绑定 IME 事件
    /// </summary>
    public void ScanInputFields()
    {
        TMP_InputField[] allFields = FindObjectsOfType<TMP_InputField>(true);

        foreach (TMP_InputField field in allFields)
        {
            RegisterField(field, field.gameObject.name);
        }
    }

    private void OnFieldSelect(TMP_InputField field)
    {
        currentField = field;
        imeVisibleField = field;
#if UNITY_WEBGL && !UNITY_EDITOR
        RectTransform rt = field.GetComponent<RectTransform>();
        if (rt == null) return;

        // 获取 InputField 四个角在屏幕上的坐标
        Vector3[] corners = new Vector3[4];
        rt.GetWorldCorners(corners);
        // corners[0] = bottom-left, corners[1] = top-left, corners[2] = top-right, corners[3] = bottom-right

        // ScreenSpaceOverlay 模式下 corners 已是屏幕像素坐标
        // HTML 坐标系：左上角为原点，Y 轴向下
        float x = corners[0].x;
        float y = Screen.height - corners[1].y;
        float w = corners[3].x - corners[0].x;
        float h = corners[1].y - corners[0].y;

        // 保证最小尺寸
        w = Mathf.Max(w, 100f);
        h = Mathf.Max(h, 28f);

        string targetGoName = gameObject.name;
        if (!fieldMap.TryGetValue(field, out string fieldId))
            fieldId = field.gameObject.name;

        SmartLabIME_Show(
            Mathf.RoundToInt(x),
            Mathf.RoundToInt(y),
            Mathf.RoundToInt(w),
            Mathf.RoundToInt(h),
            field.text ?? "",
            targetGoName,
            fieldId
        );
#endif
    }

    private void OnFieldDeselect(TMP_InputField field)
    {
        if (field == imeVisibleField && IsVisible)
        {
            currentField = field;
            return;
        }

        if (currentField == field)
            currentField = null;

        if (imeVisibleField == field)
            imeVisibleField = null;
#if UNITY_WEBGL && !UNITY_EDITOR
        SmartLabIME_Hide();
#endif
    }

    private void Update()
    {
#if UNITY_WEBGL && !UNITY_EDITOR
        // 轮询检查 IME 输入是否完成
        if (SmartLabIME_HasText() != 0)
        {
            System.IntPtr ptr = SmartLabIME_GetText();
            string data = Marshal.PtrToStringUTF8(ptr);
            // 注意：内存由 jslib malloc 分配，C# 侧无需释放（jslib 已在 GetText 中重置状态）

            if (data != null)
            {
                ProcessIMEText(data);
            }
        }
#endif
        if (Time.unscaledTime >= nextFieldScanTime)
        {
            nextFieldScanTime = Time.unscaledTime + 1.5f;
            ScanInputFields();
        }

        EnsureSelectedFieldFocused();
    }

    private void EnsureSelectedFieldFocused()
    {
        if (EventSystem.current == null)
            return;

        GameObject selected = EventSystem.current.currentSelectedGameObject;
        if (selected == null || (selected == lastSelectedObject && currentField != null))
            return;

        lastSelectedObject = selected;
        TMP_InputField selectedField = selected.GetComponent<TMP_InputField>();
        if (selectedField == null)
            selectedField = selected.GetComponentInParent<TMP_InputField>();

        if (selectedField == null)
            return;

        RegisterField(selectedField, selectedField.gameObject.name);
        if (currentField != selectedField)
            OnFieldSelect(selectedField);
    }

    private void ProcessIMEText(string data)
    {
        // 解析 "text|fieldName" 格式
        int separatorIndex = data.LastIndexOf('|');
        string text;
        string fieldName = "";

        if (separatorIndex >= 0)
        {
            text = data.Substring(0, separatorIndex);
            fieldName = data.Substring(separatorIndex + 1);
        }
        else
        {
            text = data;
        }

        Debug.Log($"[WebGLIMEHandler] IME 输入完成: '{text}', 字段: '{fieldName}'");

        // 查找对应的 TMP_InputField
        TMP_InputField targetField = null;
        foreach (var kv in fieldMap)
        {
            if (kv.Value == fieldName)
            {
                targetField = kv.Key;
                break;
            }
        }

        if (targetField == null)
            targetField = currentField;

        if (targetField != null)
        {
            targetField.text = text;
            targetField.ForceLabelUpdate();
            targetField.onValueChanged?.Invoke(text);
            targetField.onEndEdit?.Invoke(text);
        }

        currentField = null;
        imeVisibleField = null;
    }

    /// <summary>
    /// 手动注册一个 InputField（用于动态创建的 UI）
    /// </summary>
    public static void Register(TMP_InputField field, string fieldName = null)
    {
        if (field == null)
            return;

        if (instance == null)
        {
            GameObject go = new GameObject("IMEHandler");
            go.AddComponent<WebGLIMEHandler>();
            DontDestroyOnLoad(go);
        }

        instance.RegisterField(field, fieldName);
    }

    public static void Focus(TMP_InputField field)
    {
        if (field == null)
            return;

        Register(field, field.gameObject.name);
        if (instance != null)
            instance.OnFieldSelect(field);
    }

    public void RegisterField(TMP_InputField field, string fieldName)
    {
        if (field == null) return;

        field.shouldHideMobileInput = true;

        SmartLabIMEInputRelay relay = field.GetComponent<SmartLabIMEInputRelay>();
        if (relay == null)
            relay = field.gameObject.AddComponent<SmartLabIMEInputRelay>();
        relay.Bind(field);

        if (fieldMap.ContainsKey(field)) return;

        string safeName = string.IsNullOrEmpty(fieldName) ? field.gameObject.name : fieldName;
        if (string.IsNullOrEmpty(safeName))
            safeName = $"InputField_{fieldMap.Count}";

        string uniqueName = safeName;
        int suffix = 1;
        while (fieldMap.ContainsValue(uniqueName))
        {
            suffix++;
            uniqueName = $"{safeName}_{suffix}";
        }

        fieldMap[field] = uniqueName;
        field.onSelect.AddListener((text) => OnFieldSelect(field));
        field.onDeselect.AddListener((text) => OnFieldDeselect(field));

        Debug.Log($"[WebGLIMEHandler] 已绑定: {uniqueName}");
    }
}

public sealed class SmartLabIMEInputRelay : MonoBehaviour, IPointerClickHandler, ISelectHandler
{
    private TMP_InputField field;

    public void Bind(TMP_InputField inputField)
    {
        field = inputField;
    }

    public void OnPointerClick(PointerEventData eventData)
    {
        WebGLIMEHandler.Focus(ResolveField());
    }

    public void OnSelect(BaseEventData eventData)
    {
        WebGLIMEHandler.Focus(ResolveField());
    }

    private TMP_InputField ResolveField()
    {
        if (field == null)
            field = GetComponent<TMP_InputField>();
        return field;
    }
}
