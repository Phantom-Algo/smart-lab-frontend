using UnityEngine;
using TMPro;

/// <summary>
/// 出口触发器：检测玩家靠近门/出口，弹出提示，按 Q 退出游戏
/// 使用方法：
/// 1. 将此脚本挂载到门/出口 GameObject 上
/// 2. 确保该对象有 Collider（勾选 IsTrigger），或依赖距离检测
/// 3. 菜单 Tools → 搭建退出提示 UI 自动生成并绑定 HUD
/// </summary>
public class ExitTrigger : MonoBehaviour
{
    [Header("触发器设置")]
    public float triggerDistance = 3f;
    public string playerTag = "Player";

    [Header("UI 引用")]
    [Tooltip("退出提示 HUD（由 Builder 自动绑定）")]
    public GameObject promptUI;
    [Tooltip("提示文字 TMP 组件（由 Builder 自动绑定）")]
    public TextMeshProUGUI promptText;

    private bool playerInRange = false;
    private Transform playerTransform;

    private void Start()
    {
        if (promptUI != null) promptUI.SetActive(false);
    }

    private void Update()
    {
        // 无 Collider 时使用距离检测
        if (GetComponent<Collider>() == null)
        {
            DetectByDistance();
        }

        if (playerInRange && Input.GetKeyDown(KeyCode.Q))
        {
            QuitGame();
        }
    }

    private void DetectByDistance()
    {
        if (playerTransform == null)
        {
            GameObject player = GameObject.FindGameObjectWithTag(playerTag);
            if (player != null) playerTransform = player.transform;
            if (playerTransform == null) return;
        }

        float dist = Vector3.Distance(transform.position, playerTransform.position);
        bool inRange = dist <= triggerDistance;

        if (inRange != playerInRange)
        {
            playerInRange = inRange;
            if (promptUI != null) promptUI.SetActive(inRange);
        }
    }

    private void OnTriggerEnter(Collider other)
    {
        if (!other.CompareTag(playerTag)) return;
        playerInRange = true;
        if (promptUI != null) promptUI.SetActive(true);
    }

    private void OnTriggerExit(Collider other)
    {
        if (!other.CompareTag(playerTag)) return;
        playerInRange = false;
        if (promptUI != null) promptUI.SetActive(false);
    }

    private void QuitGame()
    {
#if UNITY_EDITOR
        UnityEditor.EditorApplication.isPlaying = false;
#else
        Application.Quit();
#endif
    }
}
