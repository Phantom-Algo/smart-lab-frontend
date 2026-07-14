using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// 实验室设备监控面板控制器
/// Tab 0 — 环境概览（6 张传感器数据卡片 + 设备实时状态列表）
/// Tab 1 — 设备状态（设备实时状态详细表格）
/// Tab 2 — 告警记录（时间线）
/// </summary>
public class MonitorPanelUI : MonoBehaviour
{
    [Header("=== 顶部标题栏 ===")]
    public TextMeshProUGUI titleText;
    public Button closeButton;
    public TextMeshProUGUI lastUpdateText;

    [Header("=== Tab 切换 ===")]
    public Button tabOverviewButton;
    public Button tabDeviceButton;
    public Button tabAlertButton;
    public GameObject tabOverviewHighlight;
    public GameObject tabDeviceHighlight;
    public GameObject tabAlertHighlight;
    public Color tabActiveColor = new Color(0.09f, 0.13f, 0.19f);
    public Color tabInactiveColor = new Color(0.14f, 0.19f, 0.28f);

    [Header("=== Tab 0: 环境概览 — 传感器卡片 ===")]
    public GameObject overviewPanel;
    // 6 张环境指标卡片（由 Builder 绑定）
    public TextMeshProUGUI tempValueText;
    public TextMeshProUGUI tempStatusText;
    public Image tempStatusBadge;

    public TextMeshProUGUI humidityValueText;
    public TextMeshProUGUI humidityStatusText;
    public Image humidityStatusBadge;

    public TextMeshProUGUI smokeValueText;
    public TextMeshProUGUI smokeStatusText;
    public Image smokeStatusBadge;

    public TextMeshProUGUI gasValueText;
    public TextMeshProUGUI gasStatusText;
    public Image gasStatusBadge;

    public TextMeshProUGUI batteryValueText;
    public TextMeshProUGUI batteryStatusText;
    public Image batteryStatusBadge;

    public TextMeshProUGUI signalValueText;
    public TextMeshProUGUI signalStatusText;
    public Image signalStatusBadge;

    public TextMeshProUGUI overallStatusText;
    public Image overallStatusBadge;

    [Header("=== Tab 0: 设备状态简表 ===")]
    public Transform deviceTableContainer;
    public GameObject deviceRowPrefab;

    [Header("=== Tab 0: 最近告警 ===")]
    public Transform overviewAlertListContainer;
    public GameObject overviewAlertRowPrefab;

    [Header("=== Tab 1: 设备状态详情 ===")]
    public GameObject devicePanel;
    public Transform deviceDetailContainer;
    public GameObject deviceDetailRowPrefab;

    [Header("=== Tab 2: 告警记录 ===")]
    public GameObject alertPanel;
    public Transform alertListContainer;
    public GameObject alertRowPrefab;

    [Header("=== 数据源 ===")]
    public ReagentDataProvider dataProvider;

    [Header("=== 自动刷新 ===")]
    [Tooltip("模拟 IoT 数据自动刷新间隔（秒）")]
    public float refreshInterval = 5f;

    private int currentTab = 0;
    private Coroutine refreshCoroutine;

    private void Awake()
    {
        if (Application.isPlaying)
            gameObject.SetActive(false);
    }

    private void Start() => BindEvents();

    private void OnEnable()
    {
        BindEvents();
        EnsureDataProvider();
        NormalizeOverviewLayout();
        SwitchTab(0);
        if (refreshInterval > 0f)
            refreshCoroutine = StartCoroutine(AutoRefresh());
    }

    private void OnRectTransformDimensionsChange()
    {
        if (!isActiveAndEnabled)
            return;

        NormalizeOverviewLayout();
    }

    public void RefreshData()
    {
        EnsureDataProvider();
        NormalizeOverviewLayout();
        switch (currentTab)
        {
            case 0: RefreshOverview(); break;
            case 1: RefreshDeviceDetail(); break;
            case 2: RefreshAlerts(); break;
        }
    }

    private void OnDisable()
    {
        if (refreshCoroutine != null) { StopCoroutine(refreshCoroutine); refreshCoroutine = null; }
    }

    private void EnsureDataProvider()
    {
        if (dataProvider == null)
            dataProvider = FindObjectOfType<ReagentDataProvider>();
    }

    private IEnumerator AutoRefresh()
    {
        while (true)
        {
            yield return new WaitForSeconds(refreshInterval);
            if (dataProvider != null)
                dataProvider.AdvanceMockTelemetry();

            RefreshData();
        }
    }

    private void BindEvents()
    {
        if (closeButton != null) { closeButton.onClick.RemoveAllListeners(); closeButton.onClick.AddListener(ClosePanel); }
        if (tabOverviewButton != null) { tabOverviewButton.onClick.RemoveAllListeners(); tabOverviewButton.onClick.AddListener(() => SwitchTab(0)); }
        if (tabDeviceButton != null)  { tabDeviceButton.onClick.RemoveAllListeners(); tabDeviceButton.onClick.AddListener(() => SwitchTab(1)); }
        if (tabAlertButton != null)   { tabAlertButton.onClick.RemoveAllListeners(); tabAlertButton.onClick.AddListener(() => SwitchTab(2)); }
    }

    // ─────────────────────────────────────────────────────────
    //  Tab 管理
    // ─────────────────────────────────────────────────────────

    private void SwitchTab(int index)
    {
        currentTab = index;
        if (overviewPanel != null) overviewPanel.SetActive(index == 0);
        if (devicePanel != null)   devicePanel.SetActive(index == 1);
        if (alertPanel != null)    alertPanel.SetActive(index == 2);

        UpdateTabHighlights(index);

        switch (index)
        {
            case 0: RefreshOverview(); break;
            case 1: RefreshDeviceDetail(); break;
            case 2: RefreshAlerts(); break;
        }
    }

    private void UpdateTabHighlights(int active)
    {
        UpdateTabStyle(tabOverviewButton, tabOverviewHighlight, active == 0);
        UpdateTabStyle(tabDeviceButton,  tabDeviceHighlight,  active == 1);
        UpdateTabStyle(tabAlertButton,   tabAlertHighlight,   active == 2);
    }

    private void UpdateTabStyle(Button btn, GameObject highlight, bool isActive)
    {
        if (btn != null)
        {
            var img = btn.GetComponent<Image>();
            if (img != null) img.color = isActive ? tabActiveColor : tabInactiveColor;
            var txt = btn.GetComponentInChildren<TextMeshProUGUI>();
            if (txt != null) txt.color = isActive ? Color.white : new Color(0.71f, 0.74f, 0.79f);
        }
        if (highlight != null) highlight.SetActive(isActive);
    }

    // ─────────────────────────────────────────────────────────
    //  Tab 0 — 环境概览
    // ─────────────────────────────────────────────────────────

    private void RefreshOverview()
    {
        if (dataProvider == null) return;

        NormalizeOverviewLayout();

        LabIoTData iot = dataProvider.GetIoTData();

        // 更新时间戳
        if (lastUpdateText != null)
            lastUpdateText.text = $"最后更新：{System.DateTime.Now:HH:mm:ss}";

        // 温度卡片
        UpdateSensorCard(tempValueText, tempStatusText, tempStatusBadge,
            $"{iot.temperature:F1} °C", iot.TemperatureStatus);

        // 湿度卡片
        UpdateSensorCard(humidityValueText, humidityStatusText, humidityStatusBadge,
            $"{iot.humidity:F0} %", iot.HumidityStatus);

        // 烟雾卡片
        string smokeStatus = iot.smoke > 0.1f ? "alarm" : iot.smoke > 0.05f ? "warning" : "normal";
        UpdateSensorCard(smokeValueText, smokeStatusText, smokeStatusBadge,
            $"{iot.smoke:F3} ppm", smokeStatus);

        // 气体卡片
        string gasStatus = iot.gas > 0.1f ? "alarm" : iot.gas > 0.05f ? "warning" : "normal";
        UpdateSensorCard(gasValueText, gasStatusText, gasStatusBadge,
            $"{iot.gas:F3} ppm", gasStatus);

        UpdateSensorCard(batteryValueText, batteryStatusText, batteryStatusBadge,
            $"{iot.battery:F0} %", iot.BatteryStatus);

        UpdateSensorCard(signalValueText, signalStatusText, signalStatusBadge,
            $"{iot.SignalStrengthLabel} / {iot.signalStrength:F0}%", iot.SignalStatus);

        // 综合状态
        UpdateSensorCard(null, overallStatusText, overallStatusBadge, null, iot.status);

        // 设备简表
        RefreshDeviceSimpleTable();

        // 最近告警
        RefreshOverviewAlerts();

    }

    private void UpdateSensorCard(TextMeshProUGUI valueText, TextMeshProUGUI statusText, Image statusBadge, string value, string status)
    {
        if (valueText != null && value != null) valueText.text = value;

        string statusLabel = status switch { "alarm" => "告警", "warning" => "预警", _ => "正常" };
        string statusHex   = status switch { "alarm" => "#E74C3C", "warning" => "#F39C12", _ => "#27AE60" };

        if (statusText != null) statusText.text = statusLabel;
        if (statusBadge != null && ColorUtility.TryParseHtmlString(statusHex, out Color c))
            statusBadge.color = c;
    }

    private void RefreshDeviceSimpleTable()
    {
        if (dataProvider == null || deviceTableContainer == null || deviceRowPrefab == null) return;

        foreach (Transform child in deviceTableContainer) Destroy(child.gameObject);

        foreach (var device in dataProvider.GetAllDevices())
        {
            GameObject row = Instantiate(deviceRowPrefab, deviceTableContainer);
            row.SetActive(true);
            PopulateDeviceRow(row, device, simple: true);
        }
    }

    private void RefreshOverviewAlerts()
    {
        if (dataProvider == null || overviewAlertListContainer == null || overviewAlertRowPrefab == null)
            return;

        foreach (Transform child in overviewAlertListContainer) Destroy(child.gameObject);

        List<AlertRecord> alerts = dataProvider.GetAlerts();
        int count = Mathf.Min(3, alerts.Count);
        for (int i = 0; i < count; i++)
        {
            AlertRecord alert = alerts[i];
            GameObject row = Instantiate(overviewAlertRowPrefab, overviewAlertListContainer);
            row.SetActive(true);
            SetText(row, "TimeText", alert.timeStr);
            SetText(row, "DeviceText", alert.deviceName);
            SetText(row, "MessageText", alert.message);
            SetText(row, "LevelIconText", alert.LevelIcon);
            SetText(row, "LevelText", alert.level switch { "alarm" => "告警", "warning" => "预警", "resolved" => "已恢复", _ => "状态" });

            var levelImg = FindInChildren<Image>(row, "LevelBadge");
            if (levelImg != null && ColorUtility.TryParseHtmlString(alert.LevelColor, out Color badgeColor))
                levelImg.color = badgeColor;

            var iconText = FindInChildren<TextMeshProUGUI>(row, "LevelIconText");
            if (iconText != null && ColorUtility.TryParseHtmlString(alert.LevelColor, out Color iconColor))
                iconText.color = iconColor;

            NormalizeAlertRowLayout(row, alert.message, compact: true);
        }
    }

    private void NormalizeOverviewLayout()
    {
        if (overviewPanel == null)
            return;

        RectTransform sensorCard = overviewPanel.transform.Find("SensorCard") as RectTransform;
        if (sensorCard != null)
        {
            sensorCard.anchorMin = new Vector2(0f, 1f);
            sensorCard.anchorMax = new Vector2(1f, 1f);
            sensorCard.pivot = new Vector2(0.5f, 1f);
            sensorCard.sizeDelta = new Vector2(0f, 250f);
            sensorCard.anchoredPosition = Vector2.zero;
        }

        ApplyOverviewBottomCardLayout(overviewPanel.transform.Find("DeviceSummaryCard") as RectTransform, 0f, 0.56f);
        ApplyOverviewBottomCardLayout(overviewPanel.transform.Find("OverviewAlertCard") as RectTransform, 0.58f, 1f);
    }

    private void ApplyOverviewBottomCardLayout(RectTransform cardRect, float minX, float maxX)
    {
        if (cardRect == null)
            return;

        cardRect.anchorMin = new Vector2(minX, 0f);
        cardRect.anchorMax = new Vector2(maxX, 1f);
        cardRect.pivot = new Vector2(0f, 0f);
        cardRect.offsetMin = Vector2.zero;
        cardRect.offsetMax = new Vector2(0f, -270f);
    }

    // ─────────────────────────────────────────────────────────
    //  Tab 1 — 设备状态详情
    // ─────────────────────────────────────────────────────────

    private void RefreshDeviceDetail()
    {
        if (dataProvider == null || deviceDetailContainer == null || deviceDetailRowPrefab == null) return;

        foreach (Transform child in deviceDetailContainer) Destroy(child.gameObject);

        foreach (var device in dataProvider.GetAllDevices())
        {
            GameObject row = Instantiate(deviceDetailRowPrefab, deviceDetailContainer);
            row.SetActive(true);
            PopulateDeviceRow(row, device, simple: false);
        }
    }

    private void PopulateDeviceRow(GameObject row, DeviceData device, bool simple)
    {
        SetText(row, "DeviceNameText", device.name);
        SetText(row, "StatusText", device.deviceStatus);
        SetText(row, "HealthText", $"{device.healthScore}");

        var statusImg = FindInChildren<Image>(row, "StatusBadge");
        if (statusImg != null && ColorUtility.TryParseHtmlString(device.StatusColor, out Color c))
            statusImg.color = c;

        if (device.sensorData != null)
        {
            string temp = device.sensorData.temperature >= 0 ? $"{device.sensorData.temperature:F1}°C" : "—";
            string hum  = device.sensorData.humidity >= 0 ? $"{device.sensorData.humidity:F0}%" : "—";
            string bat  = device.sensorData.battery >= 0 ? $"{device.sensorData.battery:F0}%" : "—";
            SetText(row, "TempText", temp);
            SetText(row, "HumidityText", hum);
            SetText(row, "BatteryText", bat);

            // 异常行高亮
            if (device.sensorData.status == "alarm")
            {
                var rowImg = row.GetComponent<Image>();
                if (rowImg != null) rowImg.color = new Color(0.91f, 0.3f, 0.24f, 0.12f);
            }
            else if (device.sensorData.status == "warning")
            {
                var rowImg = row.GetComponent<Image>();
                if (rowImg != null) rowImg.color = new Color(0.95f, 0.62f, 0.07f, 0.10f);
            }
        }
        else
        {
            SetText(row, "TempText", "—");
            SetText(row, "HumidityText", "—");
            SetText(row, "BatteryText", "—");
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Tab 2 — 告警记录
    // ─────────────────────────────────────────────────────────

    private void RefreshAlerts()
    {
        if (dataProvider == null || alertListContainer == null || alertRowPrefab == null) return;

        foreach (Transform child in alertListContainer) Destroy(child.gameObject);

        foreach (var alert in dataProvider.GetAlerts())
        {
            GameObject row = Instantiate(alertRowPrefab, alertListContainer);
            row.SetActive(true);

            SetText(row, "TimeText", alert.timeStr);
            SetText(row, "DeviceText", alert.deviceName);
            SetText(row, "MessageText", alert.message);
            SetText(row, "LevelIconText", alert.LevelIcon);
            SetText(row, "LevelText", alert.level switch { "alarm" => "告警", "warning" => "预警", "resolved" => "已解决", _ => "" });

            var levelImg = FindInChildren<Image>(row, "LevelBadge");
            if (levelImg != null && ColorUtility.TryParseHtmlString(alert.LevelColor, out Color c))
                levelImg.color = c;

            var iconText = FindInChildren<TextMeshProUGUI>(row, "LevelIconText");
            if (iconText != null && ColorUtility.TryParseHtmlString(alert.LevelColor, out Color ic))
                iconText.color = ic;

            NormalizeAlertRowLayout(row, alert.message, compact: false);
        }
    }

    private void NormalizeAlertRowLayout(GameObject row, string message, bool compact)
    {
        if (row == null)
            return;

        Transform levelWrap = row.transform.Find("LevelWrap");
        Transform timeWrap = row.transform.Find("TimeTextWrap");
        Transform deviceWrap = row.transform.Find("DeviceTextWrap");
        Transform messageWrap = row.transform.Find("MessageTextWrap");
        if (levelWrap == null || timeWrap == null || deviceWrap == null || messageWrap == null)
            return;

        SetLayoutWidth(levelWrap.gameObject, compact ? 92f : 108f, compact ? 80f : 96f, 0f);
        SetLayoutWidth(timeWrap.gameObject, compact ? 66f : 76f, compact ? 60f : 70f, 0f);

        Transform detailWrap = row.transform.Find("DetailWrap");
        if (detailWrap == null)
        {
            GameObject detailWrapGo = new GameObject("DetailWrap", typeof(RectTransform));
            detailWrapGo.transform.SetParent(row.transform, false);
            detailWrap = detailWrapGo.transform;

            VerticalLayoutGroup detailLayout = detailWrapGo.AddComponent<VerticalLayoutGroup>();
            detailLayout.spacing = 2f;
            detailLayout.childAlignment = TextAnchor.UpperLeft;
            detailLayout.childControlWidth = true;
            detailLayout.childControlHeight = true;
            detailLayout.childForceExpandWidth = true;
            detailLayout.childForceExpandHeight = false;

            LayoutElement detailElement = detailWrapGo.AddComponent<LayoutElement>();
            detailElement.flexibleWidth = 1f;

            detailWrap.SetSiblingIndex(row.transform.childCount - 1);
            deviceWrap.SetParent(detailWrap, false);
            messageWrap.SetParent(detailWrap, false);
        }

        SetLayoutWidth(deviceWrap.gameObject, -1f, 0f, 1f);
        SetLayoutWidth(messageWrap.gameObject, -1f, 0f, 1f);

        TextMeshProUGUI deviceText = FindInChildren<TextMeshProUGUI>(row, "DeviceText");
        if (deviceText != null)
        {
            deviceText.fontWeight = FontWeight.SemiBold;
            deviceText.enableWordWrapping = false;
            deviceText.fontSize = compact ? 12f : 13f;
        }

        TextMeshProUGUI messageText = FindInChildren<TextMeshProUGUI>(row, "MessageText");
        if (messageText != null)
        {
            messageText.enableWordWrapping = true;
            messageText.alignment = TextAlignmentOptions.TopLeft;
            messageText.fontSize = compact ? 11.5f : 12f;
            messageText.color = new Color(0.84f, 0.87f, 0.92f);

            float measureWidth = compact ? 280f : 520f;
            float preferredMessageHeight = messageText.GetPreferredValues(message, measureWidth, 0f).y;
            float targetHeight = Mathf.Max(compact ? 72f : 64f, preferredMessageHeight + 42f);

            LayoutElement rowLayout = row.GetComponent<LayoutElement>();
            if (rowLayout != null)
            {
                rowLayout.preferredHeight = targetHeight;
                rowLayout.minHeight = targetHeight;
            }
        }

        LayoutRebuilder.ForceRebuildLayoutImmediate(row.transform as RectTransform);
    }

    private void SetLayoutWidth(GameObject target, float preferredWidth, float minWidth, float flexibleWidth)
    {
        if (target == null)
            return;

        LayoutElement layout = target.GetComponent<LayoutElement>();
        if (layout == null)
            layout = target.AddComponent<LayoutElement>();

        layout.preferredWidth = preferredWidth;
        layout.minWidth = minWidth;
        layout.flexibleWidth = flexibleWidth;
    }

    // ─────────────────────────────────────────────────────────
    //  模拟 IoT 数据波动（在 Awake/Start 中使用，营造真实感）
    // ─────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────
    //  关闭面板
    // ─────────────────────────────────────────────────────────

    public void ClosePanel()
    {
        gameObject.SetActive(false);
        NotifyOwningTrigger();
        var playerMove = FindObjectOfType<PlayerMove>(true);
        if (playerMove != null) playerMove.inputEnabled = true;
        Cursor.lockState = CursorLockMode.Locked;
        Cursor.visible = false;
    }

    // ─────────────────────────────────────────────────────────
    //  工具方法
    // ─────────────────────────────────────────────────────────

    private void SetText(GameObject root, string childName, string value)
    {
        var t = FindInChildren<TextMeshProUGUI>(root, childName);
        if (t != null) t.text = value;
    }

    private T FindInChildren<T>(GameObject root, string name) where T : Component
    {
        foreach (var t in root.GetComponentsInChildren<T>(true))
            if (t.gameObject.name == name) return t;
        return null;
    }

    private void NotifyOwningTrigger()
    {
        foreach (var trigger in FindObjectsOfType<ScreenTrigger>(true))
        {
            if (trigger != null && trigger.labInfoPanel == gameObject)
            {
                trigger.NotifyPanelClosed();
                break;
            }
        }
    }
}
