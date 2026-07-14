using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// 实验室借用申请表单控制器
/// 包含：
///   - 实验室选择下拉
///   - 日期选择
///   - 时段选择
///   - 用途说明
///   - 使用人数
///   - 提交按钮
/// </summary>
public class LabBorrowFormUI : MonoBehaviour
{
    private static readonly List<string> DefaultTimeSlots = new List<string>
    {
        "08:00 - 10:00",
        "10:00 - 12:00",
        "14:00 - 16:00",
        "16:00 - 18:00",
        "18:00 - 20:00",
        "20:00 - 22:00"
    };

    [Header("=== 表单字段 ===")]
    public TMP_Dropdown labDropdown;           // 实验室选择下拉
    public TMP_InputField applicantNameInput;  // 申请人姓名
    public TMP_InputField dateInput;           // 日期输入（可后续替换为 DatePicker）
    public TMP_Dropdown timeSlotDropdown;      // 时段下拉
    public TMP_InputField purposeInput;        // 用途说明
    public TMP_InputField peopleCountInput;    // 使用人数
    public Button submitButton;                // 提交按钮
    public Button clearButton;                 // 清空按钮

    [Header("=== 状态提示 ===")]
    public TextMeshProUGUI statusMessage;      // 状态/错误信息
    public GameObject successOverlay;          // 提交成功覆盖层
    public TextMeshProUGUI successText;

    [Header("=== 时段选项 ===")]
    public List<string> timeSlotOptions = new List<string>
    {
        "08:00 - 10:00",
        "10:00 - 12:00",
        "14:00 - 16:00",
        "16:00 - 18:00",
        "18:00 - 20:00",
        "20:00 - 22:00"
    };

    /// <summary>当前可选的实验室列表</summary>
    private List<LabInfoData> availableLabs = new List<LabInfoData>();

    /// <summary>当前选中的实验室 ID</summary>
    private int selectedLabId = 0;

    private int timeSlotRequestVersion = 0;

    private void Awake()
    {
        EnsureFormReferences();
    }

    private void OnEnable()
    {
        EnsureFormReferences();
        WebGLBridge.ContextChanged += HandleWebContextChanged;
        SmartLabDropdownRuntimeFix.Normalize(labDropdown);
        SmartLabDropdownRuntimeFix.Normalize(timeSlotDropdown);
        PopulateTimeSlotOptions();
        EnsureLabPlaceholder();
        ApplyApplicantNameFromContext(onlyWhenEmpty: true);
    }

    private void OnDisable()
    {
        WebGLBridge.ContextChanged -= HandleWebContextChanged;
    }

    private void Start()
    {
        EnsureFormReferences();
        PopulateTimeSlotOptions();

        EnsureLabPlaceholder();

        ApplyApplicantNameFromContext(onlyWhenEmpty: true);

        // 绑定按钮事件
        if (submitButton != null) submitButton.onClick.AddListener(OnSubmit);
        if (clearButton != null) clearButton.onClick.AddListener(ClearForm);
        if (dateInput != null)
        {
            dateInput.onEndEdit.RemoveListener(OnDateEdited);
            dateInput.onEndEdit.AddListener(OnDateEdited);
        }

        // 初始隐藏成功覆盖层
        if (successOverlay != null) successOverlay.SetActive(false);
        if (statusMessage != null) statusMessage.gameObject.SetActive(false);
    }

    private void EnsureFormReferences()
    {
        if (labDropdown == null) labDropdown = FindComponentByName<TMP_Dropdown>("LabDropdown");
        if (timeSlotDropdown == null) timeSlotDropdown = FindComponentByName<TMP_Dropdown>("TimeSlotDropdown");
        if (applicantNameInput == null) applicantNameInput = FindComponentByName<TMP_InputField>("ApplicantNameInput");
        if (dateInput == null) dateInput = FindComponentByName<TMP_InputField>("DateInput");
        if (purposeInput == null) purposeInput = FindComponentByName<TMP_InputField>("PurposeInput");
        if (peopleCountInput == null) peopleCountInput = FindComponentByName<TMP_InputField>("PeopleCountInput");
        if (submitButton == null) submitButton = FindComponentByName<Button>("SubmitButton");
        if (clearButton == null) clearButton = FindComponentByName<Button>("ClearButton");
        if (statusMessage == null) statusMessage = FindComponentByName<TextMeshProUGUI>("StatusMessage");
        if (successText == null) successText = FindComponentByName<TextMeshProUGUI>("SuccessText");
        if (successOverlay == null)
        {
            Transform overlay = FindChildRecursive(transform, "SuccessOverlay");
            if (overlay != null) successOverlay = overlay.gameObject;
        }

        WebGLIMEHandler.Register(applicantNameInput, "ApplicantNameInput");
        WebGLIMEHandler.Register(dateInput, "DateInput");
        WebGLIMEHandler.Register(purposeInput, "PurposeInput");
        WebGLIMEHandler.Register(peopleCountInput, "PeopleCountInput");
    }

    private T FindComponentByName<T>(string objectName) where T : Component
    {
        Transform child = FindChildRecursive(transform, objectName);
        return child != null ? child.GetComponent<T>() : null;
    }

    private static Transform FindChildRecursive(Transform root, string objectName)
    {
        if (root == null || string.IsNullOrEmpty(objectName))
            return null;

        if (root.name == objectName)
            return root;

        for (int i = 0; i < root.childCount; i++)
        {
            Transform found = FindChildRecursive(root.GetChild(i), objectName);
            if (found != null)
                return found;
        }

        return null;
    }

    private void HandleWebContextChanged(SmartLabWebContext _)
    {
        ApplyApplicantNameFromContext(onlyWhenEmpty: true);
    }

    private void PopulateTimeSlotOptions()
    {
        SetTimeSlotOptions(timeSlotOptions);
    }

    private void SetTimeSlotOptions(List<string> options)
    {
        if (timeSlotDropdown == null)
            return;

        List<string> safeOptions = options != null && options.Count > 0
            ? new List<string>(options)
            : new List<string>(DefaultTimeSlots);

        timeSlotDropdown.ClearOptions();
        timeSlotDropdown.AddOptions(safeOptions);
        timeSlotDropdown.SetValueWithoutNotify(0);
        timeSlotDropdown.RefreshShownValue();
        SmartLabDropdownRuntimeFix.Normalize(timeSlotDropdown);
    }

    private void EnsureLabPlaceholder()
    {
        if (labDropdown == null || labDropdown.options.Count > 0)
            return;

        labDropdown.ClearOptions();
        labDropdown.AddOptions(new List<string> { "等待数据加载…" });
        labDropdown.SetValueWithoutNotify(0);
        labDropdown.RefreshShownValue();
    }

    private void ApplyApplicantNameFromContext(bool onlyWhenEmpty)
    {
        if (applicantNameInput == null)
            return;

        string currentUserName = ResolveCurrentUserName();
        if (string.IsNullOrWhiteSpace(currentUserName))
            return;

        if (onlyWhenEmpty && !string.IsNullOrWhiteSpace(applicantNameInput.text))
            return;

        applicantNameInput.SetTextWithoutNotify(currentUserName);
    }

    private static string ResolveCurrentUserName()
    {
        return WebGLBridge.CurrentContext != null ? WebGLBridge.CurrentContext.userName : string.Empty;
    }

    /// <summary>
    /// 刷新可用实验室列表（由 LabInfoPanelUI 调用）
    /// </summary>
    public void RefreshLabList(List<LabInfoData> labs)
    {
        EnsureFormReferences();
        PopulateTimeSlotOptions();
        availableLabs = labs ?? new List<LabInfoData>();

        if (labDropdown == null) return;

        labDropdown.ClearOptions();
        List<string> options = new List<string>();

        foreach (var lab in availableLabs)
        {
            string option = $"{lab.labName} ({lab.labCode}) — {lab.StatusName}";
            options.Add(option);
        }

        if (options.Count == 0)
        {
            options.Add("暂无可用实验室");
        }

        labDropdown.AddOptions(options);
        labDropdown.SetValueWithoutNotify(0);
        labDropdown.RefreshShownValue();
        SmartLabDropdownRuntimeFix.Normalize(labDropdown);

        // 绑定选择事件
        labDropdown.onValueChanged.RemoveListener(OnLabSelected);
        labDropdown.onValueChanged.AddListener(OnLabSelected);

        // 默认选中第一个
        selectedLabId = availableLabs.Count > 0 ? availableLabs[0].id : 0;
        RefreshTimeSlotsForCurrentSelection();
    }

    private void OnLabSelected(int index)
    {
        if (index >= 0 && index < availableLabs.Count)
        {
            selectedLabId = availableLabs[index].id;
            RefreshTimeSlotsForCurrentSelection();
        }
    }

    private void OnDateEdited(string _)
    {
        RefreshTimeSlotsForCurrentSelection();
    }

    private void RefreshTimeSlotsForCurrentSelection()
    {
        EnsureFormReferences();
        int requestVersion = ++timeSlotRequestVersion;
        string date = dateInput != null ? dateInput.text.Trim() : "";

        if (selectedLabId <= 0 || string.IsNullOrEmpty(date))
        {
            SetTimeSlotOptions(LabDataProvider.GetDefaultReservationTimeSlots());
            return;
        }

        if (LabDataProvider.Instance == null)
        {
            SetTimeSlotOptions(LabDataProvider.GetDefaultReservationTimeSlots());
            return;
        }

        LabDataProvider.Instance.LoadReservationTimeSlots(selectedLabId, date, (slots, warning) =>
        {
            if (requestVersion != timeSlotRequestVersion)
                return;

            SetTimeSlotOptions(slots);
            if (!string.IsNullOrEmpty(warning))
                Debug.LogWarning($"[LabBorrowFormUI] 使用默认预约时段: {warning}");
        });
    }

    public void SelectLabById(int labId)
    {
        if (labDropdown == null || availableLabs == null || availableLabs.Count == 0)
        {
            return;
        }

        int targetIndex = availableLabs.FindIndex(lab => lab.id == labId);
        if (targetIndex < 0)
        {
            return;
        }

        labDropdown.SetValueWithoutNotify(targetIndex);
        selectedLabId = availableLabs[targetIndex].id;
        RefreshTimeSlotsForCurrentSelection();
    }

    /// <summary>
    /// 提交借用申请
    /// </summary>
    private void OnSubmit()
    {
        EnsureFormReferences();
        // 隐藏之前的提示
        if (statusMessage != null) statusMessage.gameObject.SetActive(false);

        // 表单校验
        string applicantName = applicantNameInput != null ? applicantNameInput.text.Trim() : "";
        string date = dateInput != null ? dateInput.text.Trim() : "";
        string timeSlot = timeSlotDropdown != null && timeSlotDropdown.options.Count > 0
            ? timeSlotDropdown.options[timeSlotDropdown.value].text
            : "";
        string purpose = purposeInput != null ? purposeInput.text.Trim() : "";
        int peopleCount = 0;
        if (peopleCountInput != null && !string.IsNullOrEmpty(peopleCountInput.text))
        {
            int.TryParse(peopleCountInput.text.Trim(), out peopleCount);
        }

        // 本地校验
        if (string.IsNullOrEmpty(applicantName))
        {
            ShowStatus("请填写申请人姓名", true);
            return;
        }
        if (selectedLabId <= 0)
        {
            ShowStatus("请选择目标实验室", true);
            return;
        }
        if (string.IsNullOrEmpty(date))
        {
            ShowStatus("请选择预约日期", true);
            return;
        }
        if (peopleCount <= 0)
        {
            ShowStatus("使用人数必须大于 0", true);
            return;
        }
        if (string.IsNullOrEmpty(purpose))
        {
            ShowStatus("请填写用途说明", true);
            return;
        }

        // 构建请求
        LabBorrowRequest request = new LabBorrowRequest
        {
            labId = selectedLabId,
            applicantName = applicantName,
            reserveDate = date,
            timeSlot = timeSlot,
            purpose = purpose,
            peopleCount = peopleCount
        };

        // 禁用提交按钮防止重复提交
        if (submitButton != null) submitButton.interactable = false;

        // 调用数据层提交
        if (LabDataProvider.Instance != null)
        {
            LabDataProvider.Instance.SubmitBorrowRequest(request, (success, message) =>
            {
                // 注意：回调可能在非主线程？这里直接在主线程协程中处理，所以是安全的
                if (submitButton != null) submitButton.interactable = true;

                if (success)
                {
                    ShowSuccess(message);
                    ClearForm();
                }
                else
                {
                    ShowStatus(message, true);
                }
            });
        }
        else
        {
            ShowStatus("系统错误：数据服务未就绪", true);
            if (submitButton != null) submitButton.interactable = true;
        }
    }

    /// <summary>
    /// 显示状态消息
    /// </summary>
    private void ShowStatus(string message, bool isError)
    {
        if (statusMessage == null) return;

        statusMessage.gameObject.SetActive(true);
        statusMessage.text = message;
        statusMessage.color = isError ? new Color(0.91f, 0.30f, 0.24f) : new Color(0.15f, 0.68f, 0.38f);

        // 3 秒后自动隐藏
        CancelInvoke(nameof(HideStatus));
        Invoke(nameof(HideStatus), 3f);
    }

    private void HideStatus()
    {
        if (statusMessage != null) statusMessage.gameObject.SetActive(false);
    }

    /// <summary>
    /// 显示提交成功
    /// </summary>
    private void ShowSuccess(string message)
    {
        if (successOverlay != null)
        {
            successOverlay.SetActive(true);
            if (successText != null) successText.text = message;
        }

        // 2.5 秒后自动隐藏成功覆盖层
        CancelInvoke(nameof(HideSuccess));
        Invoke(nameof(HideSuccess), 2.5f);
    }

    private void HideSuccess()
    {
        if (successOverlay != null) successOverlay.SetActive(false);
    }

    /// <summary>
    /// 清空表单
    /// </summary>
    public void ClearForm()
    {
        EnsureFormReferences();
        if (applicantNameInput != null) applicantNameInput.text = "";
        if (dateInput != null) dateInput.text = "";
        if (timeSlotDropdown != null)
        {
            SetTimeSlotOptions(LabDataProvider.GetDefaultReservationTimeSlots());
            timeSlotDropdown.SetValueWithoutNotify(0);
            timeSlotDropdown.RefreshShownValue();
        }
        if (purposeInput != null) purposeInput.text = "";
        if (peopleCountInput != null) peopleCountInput.text = "";
        if (labDropdown != null && availableLabs.Count > 0)
        {
            labDropdown.SetValueWithoutNotify(0);
            labDropdown.RefreshShownValue();
        }
        if (statusMessage != null) statusMessage.gameObject.SetActive(false);

        ApplyApplicantNameFromContext(onlyWhenEmpty: false);

        selectedLabId = availableLabs.Count > 0 ? availableLabs[0].id : 0;
    }
}
