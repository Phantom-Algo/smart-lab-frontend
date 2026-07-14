using UnityEngine;
using TMPro;

/// <summary>
/// 大屏触发器：检测玩家靠近，弹出交互提示，按下 Space 打开信息面板
/// 
/// 使用方法：
/// 1. 将此脚本挂载到大屏 GameObject 上
/// 2. 确保大屏有 Collider（勾选 IsTrigger）
/// 3. 在 Inspector 中拖入提示 UI 和 LabInfoPanel
/// </summary>
public class ScreenTrigger : MonoBehaviour
{
    [Header("触发器设置")]
    [Tooltip("触发检测距离（如果使用 Collider 则不需要）")]
    public float triggerDistance = 3f;

    [Header("UI 引用")]
    [Tooltip("世界空间中的提示文本（如 '按 Space 查看大屏信息'）")]
    public GameObject promptUI;
    [Tooltip("提示文本的 TextMeshPro 组件")]
    public TextMeshProUGUI promptText;
    [Tooltip("LabInfoPanel 的根 GameObject")]
    public GameObject labInfoPanel;

    [Header("提示文本内容")]
    [TextArea]
    public string promptMessage = "按 <color=#FFD700>Space</color> 查看大屏信息";

    [Header("玩家 Tag")]
    public string playerTag = "Player";

    /// <summary>玩家是否在触发范围内</summary>
    private bool playerInRange = false;

    /// <summary>面板是否已打开</summary>
    private bool panelOpen = false;

    private Transform playerTransform;
    private PlayerMove playerMove;

    private void Start()
    {
        if (promptUI != null) promptUI.SetActive(false);
        if (labInfoPanel != null) labInfoPanel.SetActive(false);

        // 自动查找场景中的 PlayerMove（包含 inactive）
        playerMove = FindObjectOfType<PlayerMove>(true);
    }

    private void Update()
    {
        // 如果没有使用 Collider 触发，则使用距离检测
        if (GetComponent<Collider>() == null)
        {
            DetectByDistance();
        }

        // Space 只用于打开面板，关闭用鼠标点击 X 按钮（避免中文输入时空格误关）
        // IME 输入活跃时不响应任何键盘操作
        if (playerInRange && !panelOpen && Input.GetKeyDown(KeyCode.Space) && !IsIMEActive())
        {
            OpenPanel();
        }

        SyncPanelState();
    }

    /// <summary>距离检测模式（当没有 Collider 时使用）</summary>
    private void DetectByDistance()
    {
        if (playerTransform == null)
        {
            GameObject player = GameObject.FindGameObjectWithTag(playerTag);
            if (player != null) playerTransform = player.transform;
        }

        if (playerTransform == null) return;

        float dist = Vector3.Distance(transform.position, playerTransform.position);
        bool wasInRange = playerInRange;
        playerInRange = dist <= triggerDistance;

        if (playerInRange && !wasInRange)
        {
            OnPlayerEnter();
        }
        else if (!playerInRange && wasInRange)
        {
            OnPlayerExit();
        }
    }

    // ==================== Collider 触发模式 ====================

    private void OnTriggerEnter(Collider other)
    {
        if (other.CompareTag(playerTag))
        {
            playerTransform = other.transform;
            playerInRange = true;
            OnPlayerEnter();
        }
    }

    private void OnTriggerExit(Collider other)
    {
        if (other.CompareTag(playerTag))
        {
            playerInRange = false;
            OnPlayerExit();
        }
    }

    // ==================== 逻辑 ====================

    private void OnPlayerEnter()
    {
        if (promptUI != null)
        {
            promptUI.SetActive(true);
            if (promptText != null)
            {
                promptText.text = promptMessage;
            }
        }

        // 如果面板打开着且玩家重新进入范围，不做额外处理
    }

    private void OnPlayerExit()
    {
        if (promptUI != null) promptUI.SetActive(false);

        // 玩家离开范围时自动关闭面板
        if (panelOpen)
        {
            ClosePanel();
        }
    }

    private void TogglePanel()
    {
        if (panelOpen)
            ClosePanel();
        else
            OpenPanel();
    }

    /// <summary>
    /// 检查 WebGL IME 输入框是否活跃（避免中文输入时空格键误关面板）
    /// </summary>
    private static bool IsIMEActive()
    {
#if UNITY_WEBGL && !UNITY_EDITOR
        return WebGLIMEHandler.IsVisible;
#else
        return false;
#endif
    }

    private void OpenPanel()
    {
        if (labInfoPanel == null)
        {
            Debug.LogWarning("[ScreenTrigger] 交互面板未绑定！");
            return;
        }

        panelOpen = true;
        labInfoPanel.SetActive(true);

        // 禁用玩家移动和视角
        SetPlayerControlEnabled(false);

        // 隐藏靠近提示
        if (promptUI != null) promptUI.SetActive(false);

        // 解锁光标
        Cursor.lockState = CursorLockMode.None;
        Cursor.visible = true;

        // 通知面板控制器刷新数据（OnEnable 已自动触发一次，此处为安全冗余）
        labInfoPanel.SendMessage("RefreshData", SendMessageOptions.DontRequireReceiver);

        WebGLBridge.EmitWebEvent("scene.panel_opened", $"已打开交互面板：{labInfoPanel.name}", "info", labInfoPanel.name);

        Debug.Log($"[ScreenTrigger] 已打开交互面板: {labInfoPanel.name}");
    }

    private void ClosePanel()
    {
        panelOpen = false;

        if (labInfoPanel != null && labInfoPanel.activeSelf)
            labInfoPanel.SetActive(false);

        RestoreInteractionState();

        if (labInfoPanel != null)
        {
            WebGLBridge.EmitWebEvent("scene.panel_closed", $"已关闭交互面板：{labInfoPanel.name}", "info", labInfoPanel.name);
        }

        Debug.Log("[ScreenTrigger] 交互面板已关闭");
    }

    public void NotifyPanelClosed()
    {
        panelOpen = false;
        RestoreInteractionState();
    }

    private void SyncPanelState()
    {
        if (!panelOpen || labInfoPanel == null)
            return;

        if (!labInfoPanel.activeSelf)
        {
            NotifyPanelClosed();
        }
    }

    private void RestoreInteractionState()
    {
        SetPlayerControlEnabled(true);

        if (playerInRange && promptUI != null)
        {
            promptUI.SetActive(true);
            if (promptText != null)
                promptText.text = promptMessage;
        }

        Cursor.lockState = CursorLockMode.Locked;
        Cursor.visible = false;
    }

    private void SetPlayerControlEnabled(bool enabled)
    {
        if (playerMove == null)
            playerMove = FindObjectOfType<PlayerMove>(true);

        if (playerMove != null)
            playerMove.inputEnabled = enabled;
    }

    // ==================== 调试 ====================

    private void OnDrawGizmosSelected()
    {
        // 可视化触发范围
        if (GetComponent<Collider>() == null)
        {
            Gizmos.color = new Color(0, 1, 1, 0.3f);
            Gizmos.DrawWireSphere(transform.position, triggerDistance);
        }
        else
        {
            Gizmos.color = new Color(0, 1, 1, 0.3f);
            Gizmos.DrawWireCube(transform.position, transform.localScale);
        }
    }
}
