using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// 实验器材管理面板控制器
/// Tab 0 — 器材列表（卡片网格 + 右侧详情）
/// Tab 1 — 借用申请
/// Tab 2 — 借用记录
/// Tab 3 — 维修上报
/// </summary>
public class EquipmentPanelUI : MonoBehaviour
{
    private const float ListCardMinWidth = 240f;
    private const int ListMinColumns = 2;
    private const int ListMaxColumns = 3;

    [Header("=== 顶部标题栏 ===")]
    public TextMeshProUGUI titleText;
    public Button closeButton;

    [Header("=== Tab 切换 ===")]
    public Button tabListButton;
    public Button tabBorrowButton;
    public Button tabRecordButton;
    public Button tabRepairButton;
    public GameObject tabListHighlight;
    public GameObject tabBorrowHighlight;
    public GameObject tabRecordHighlight;
    public GameObject tabRepairHighlight;
    public Color tabActiveColor = new Color(0.09f, 0.13f, 0.19f);
    public Color tabInactiveColor = new Color(0.14f, 0.19f, 0.28f);

    [Header("=== Tab 0: 器材列表 ===")]
    public GameObject listPanel;
    public Transform cardContainer;
    public GameObject cardPrefab;
    public TMP_InputField searchInput;
    public TMP_Dropdown categoryFilterDropdown;
    public TMP_Dropdown statusFilterDropdown;

    [Header("=== 右侧详情面板 ===")]
    public GameObject detailPanel;
    public TextMeshProUGUI detailNameText;
    public TextMeshProUGUI detailNoText;
    public TextMeshProUGUI detailSpecText;
    public TextMeshProUGUI detailLabText;
    public TextMeshProUGUI detailPurchaseDateText;
    public TextMeshProUGUI detailWarrantyText;
    public TextMeshProUGUI detailStatusText;
    public Image detailStatusBadge;
    public Slider detailHealthSlider;
    public TextMeshProUGUI detailHealthText;
    public TextMeshProUGUI detailSensorText;
    public Button detailBorrowButton;
    public Button detailRepairButton;
    public Button detailRecordButton;

    [Header("=== Tab 1: 借用申请 ===")]
    public GameObject borrowPanel;
    public TMP_Dropdown borrowDeviceDropdown;
    public TMP_InputField borrowApplicantInput;
    public TMP_InputField borrowStartInput;
    public TMP_InputField borrowEndInput;
    public TMP_InputField borrowPurposeInput;
    public Button borrowSubmitButton;
    public Button borrowClearButton;
    public TextMeshProUGUI borrowStatusText;
    public GameObject borrowSuccessOverlay;

    [Header("=== Tab 2: 借用记录 ===")]
    public GameObject recordPanel;
    public Transform recordListContainer;
    public GameObject recordRowPrefab;

    [Header("=== Tab 3: 维修上报 ===")]
    public GameObject repairPanel;
    public TMP_Dropdown repairDeviceDropdown;
    public TMP_InputField repairDescInput;
    public Button repairSubmitButton;
    public TextMeshProUGUI repairStatusText;
    public Transform repairRecordListContainer;
    public GameObject repairRecordRowPrefab;
    public TextMeshProUGUI repairHistoryPlaceholderText;

    [Header("=== 数据源 ===")]
    public ReagentDataProvider dataProvider;

    private int currentTab = 0;
    private DeviceData currentDetailDevice;
    private List<DeviceData> currentDisplayList = new List<DeviceData>();

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
        SetupFilterOptions();
        SmartLabDropdownRuntimeFix.Normalize(categoryFilterDropdown);
        SmartLabDropdownRuntimeFix.Normalize(statusFilterDropdown);
        SmartLabDropdownRuntimeFix.Normalize(borrowDeviceDropdown);
        SmartLabDropdownRuntimeFix.Normalize(repairDeviceDropdown);
        ApplyBorrowApplicantNameFromContext();
        SwitchTab(0);
    }

    private void OnRectTransformDimensionsChange()
    {
        if (!isActiveAndEnabled)
            return;

        ApplyResponsiveLayout();
    }

    private void EnsureDataProvider()
    {
        if (dataProvider == null)
            dataProvider = FindObjectOfType<ReagentDataProvider>();
    }

    private void BindEvents()
    {
        if (closeButton != null) { closeButton.onClick.RemoveAllListeners(); closeButton.onClick.AddListener(ClosePanel); }
        if (tabListButton != null) { tabListButton.onClick.RemoveAllListeners(); tabListButton.onClick.AddListener(() => SwitchTab(0)); }
        if (tabBorrowButton != null) { tabBorrowButton.onClick.RemoveAllListeners(); tabBorrowButton.onClick.AddListener(() => SwitchTab(1)); }
        if (tabRecordButton != null) { tabRecordButton.onClick.RemoveAllListeners(); tabRecordButton.onClick.AddListener(() => SwitchTab(2)); }
        if (tabRepairButton != null) { tabRepairButton.onClick.RemoveAllListeners(); tabRepairButton.onClick.AddListener(() => SwitchTab(3)); }

        if (searchInput != null) { searchInput.onValueChanged.RemoveAllListeners(); searchInput.onValueChanged.AddListener(_ => RefreshDeviceList()); }
        if (categoryFilterDropdown != null) { categoryFilterDropdown.onValueChanged.RemoveAllListeners(); categoryFilterDropdown.onValueChanged.AddListener(_ => RefreshDeviceList()); }
        if (statusFilterDropdown != null) { statusFilterDropdown.onValueChanged.RemoveAllListeners(); statusFilterDropdown.onValueChanged.AddListener(_ => RefreshDeviceList()); }

        if (borrowSubmitButton != null) { borrowSubmitButton.onClick.RemoveAllListeners(); borrowSubmitButton.onClick.AddListener(SubmitBorrow); }
        if (borrowClearButton != null) { borrowClearButton.onClick.RemoveAllListeners(); borrowClearButton.onClick.AddListener(ClearBorrowForm); }
        if (repairSubmitButton != null) { repairSubmitButton.onClick.RemoveAllListeners(); repairSubmitButton.onClick.AddListener(SubmitRepair); }

        if (detailBorrowButton != null) { detailBorrowButton.onClick.RemoveAllListeners(); detailBorrowButton.onClick.AddListener(() => SwitchTab(1)); }
        if (detailRepairButton != null) { detailRepairButton.onClick.RemoveAllListeners(); detailRepairButton.onClick.AddListener(() => SwitchTab(3)); }
        if (detailRecordButton != null) { detailRecordButton.onClick.RemoveAllListeners(); detailRecordButton.onClick.AddListener(() => SwitchTab(2)); }
    }

    public void RefreshData()
    {
        EnsureDataProvider();
        SetupFilterOptions();
        SwitchTab(currentTab);

        if (currentDetailDevice != null && dataProvider != null)
        {
            DeviceData updated = dataProvider.GetDeviceById(currentDetailDevice.id);
            if (updated != null)
                ShowDeviceDetail(updated);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Tab 管理
    // ─────────────────────────────────────────────────────────

    private void SwitchTab(int index)
    {
        currentTab = index;
        if (listPanel != null) listPanel.SetActive(index == 0);
        if (borrowPanel != null) borrowPanel.SetActive(index == 1);
        if (recordPanel != null) recordPanel.SetActive(index == 2);
        if (repairPanel != null) repairPanel.SetActive(index == 3);

        UpdateTabHighlights(index);

        switch (index)
        {
            case 0: RefreshDeviceList(); break;
            case 1: RefreshBorrowForm(); break;
            case 2: RefreshBorrowRecords(); break;
            case 3: RefreshRepairForm(); break;
        }
    }

    private void UpdateTabHighlights(int active)
    {
        UpdateTabStyle(tabListButton,   tabListHighlight,   active == 0);
        UpdateTabStyle(tabBorrowButton, tabBorrowHighlight, active == 1);
        UpdateTabStyle(tabRecordButton, tabRecordHighlight, active == 2);
        UpdateTabStyle(tabRepairButton, tabRepairHighlight, active == 3);
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
    //  Tab 0 — 器材列表
    // ─────────────────────────────────────────────────────────

    private void RefreshDeviceList()
    {
        if (dataProvider == null || cardContainer == null || cardPrefab == null) return;

        string filter = searchInput != null ? searchInput.text.Trim() : "";
        string categoryFilter = "";
        if (categoryFilterDropdown != null && categoryFilterDropdown.value > 0)
            categoryFilter = categoryFilterDropdown.options[categoryFilterDropdown.value].text;
        string statusFilter = "";
        if (statusFilterDropdown != null && statusFilterDropdown.value > 0)
            statusFilter = statusFilterDropdown.options[statusFilterDropdown.value].text;

        var all = dataProvider.GetAllDevices();
        currentDisplayList = new List<DeviceData>();
        foreach (var d in all)
        {
            bool matchFilter = string.IsNullOrEmpty(filter)
                || d.name.IndexOf(filter, System.StringComparison.OrdinalIgnoreCase) >= 0
                || d.deviceNo.IndexOf(filter, System.StringComparison.OrdinalIgnoreCase) >= 0;
            bool matchCategory = string.IsNullOrEmpty(categoryFilter) || d.category == categoryFilter;
            bool matchStatus = string.IsNullOrEmpty(statusFilter) || d.deviceStatus == statusFilter;
            if (matchFilter && matchCategory && matchStatus) currentDisplayList.Add(d);
        }

        foreach (Transform child in cardContainer) Destroy(child.gameObject);

        foreach (var device in currentDisplayList)
        {
            GameObject card = Instantiate(cardPrefab, cardContainer);
            card.SetActive(true);
            PopulateDeviceCard(card, device);
        }

        ApplyResponsiveGrid(cardContainer, ListCardMinWidth, ListMinColumns, ListMaxColumns);

        if (currentDisplayList.Count > 0)
        {
            DeviceData selected = currentDisplayList.Find(d => currentDetailDevice != null && d.id == currentDetailDevice.id) ?? currentDisplayList[0];
            ShowDeviceDetail(selected);
        }
        else if (detailPanel != null)
        {
            detailPanel.SetActive(false);
            currentDetailDevice = null;
        }
    }

    private void PopulateDeviceCard(GameObject card, DeviceData device)
    {
        SetText(card, "NameText", device.name);
        SetText(card, "NoText", device.deviceNo);
        SetText(card, "StatusText", device.deviceStatus);
        SetText(card, "HealthText", $"健康分: {device.healthScore}");

        var statusImg = FindInChildren<Image>(card, "StatusBadge");
        if (statusImg != null && ColorUtility.TryParseHtmlString(device.StatusColor, out Color c))
            statusImg.color = c;

        var healthSlider = FindInChildren<Slider>(card, "HealthSlider");
        if (healthSlider != null)
        {
            healthSlider.value = device.healthScore / 100f;
            // 健康条颜色
            var fill = healthSlider.fillRect != null ? healthSlider.fillRect.GetComponent<Image>() : null;
            if (fill != null && ColorUtility.TryParseHtmlString(device.HealthColor, out Color hc))
                fill.color = hc;
        }

        var btn = card.GetComponentInChildren<Button>();
        if (btn != null)
        {
            btn.onClick.RemoveAllListeners();
            var d = device;
            btn.onClick.AddListener(() => ShowDeviceDetail(d));
        }
    }

    private void ShowDeviceDetail(DeviceData device)
    {
        currentDetailDevice = device;
        if (detailPanel == null) return;
        detailPanel.SetActive(true);

        if (detailNameText != null) detailNameText.text = device.name;
        if (detailNoText != null) detailNoText.text = device.deviceNo;
        if (detailSpecText != null) detailSpecText.text = device.spec;
        if (detailLabText != null) detailLabText.text = device.labName;
        if (detailPurchaseDateText != null) detailPurchaseDateText.text = device.purchaseDateStr;
        if (detailStatusText != null) detailStatusText.text = device.deviceStatus;
        if (detailStatusBadge != null && ColorUtility.TryParseHtmlString(device.StatusColor, out Color c))
            detailStatusBadge.color = c;

        string warranty = device.expireDateStr;
        if (device.IsWarrantyExpired) warranty += " <color=#E74C3C>（已过保）</color>";
        if (detailWarrantyText != null) { detailWarrantyText.text = warranty; detailWarrantyText.richText = true; }

        if (detailHealthSlider != null) detailHealthSlider.value = device.healthScore / 100f;
        if (detailHealthText != null) detailHealthText.text = $"{device.healthScore}/100 · {device.HealthLabel}";

        // 传感器数据
        if (detailSensorText != null)
        {
            if (device.sensorData != null)
            {
                string temp = device.sensorData.temperature >= 0 ? $"{device.sensorData.temperature}°C" : "—";
                string hum  = device.sensorData.humidity >= 0 ? $"{device.sensorData.humidity}%" : "—";
                string bat  = device.sensorData.battery >= 0 ? $"{device.sensorData.battery}%" : "—";
                string signal = device.sensorData.signalStrength >= 0 ? $"{device.sensorData.signalStrength}%" : "—";
                detailSensorText.text = $"温度 {temp}  /  湿度 {hum}  /  电量 {bat}  /  信号 {signal}";
            }
            else
            {
                detailSensorText.text = "暂无传感器数据";
            }
        }

        // 报废/维修中设备禁用借用按钮
        bool canBorrow = device.deviceStatus == "闲置";
        if (detailBorrowButton != null) detailBorrowButton.interactable = canBorrow;
    }

    // ─────────────────────────────────────────────────────────
    //  Tab 1 — 借用申请
    // ─────────────────────────────────────────────────────────

    private void RefreshBorrowForm()
    {
        if (dataProvider == null || borrowDeviceDropdown == null) return;

        borrowDeviceDropdown.ClearOptions();
        var options = new List<string>();
        foreach (var d in dataProvider.GetAllDevices())
        {
            string label = $"{d.name}（{d.deviceNo}）— {d.deviceStatus}";
            options.Add(label);
        }
        if (options.Count == 0)
            options.Add("暂无可借设备");
        borrowDeviceDropdown.AddOptions(options);
        borrowDeviceDropdown.SetValueWithoutNotify(0);
        borrowDeviceDropdown.RefreshShownValue();

        if (borrowSuccessOverlay != null) borrowSuccessOverlay.SetActive(false);
        if (borrowStatusText != null) { borrowStatusText.text = ""; borrowStatusText.gameObject.SetActive(false); }
        ApplyBorrowApplicantNameFromContext();

        // 若从详情页跳来，预选当前设备
        if (currentDetailDevice != null)
        {
            var all = dataProvider.GetAllDevices();
            int idx = all.FindIndex(d => d.id == currentDetailDevice.id);
            if (idx >= 0) borrowDeviceDropdown.value = idx;
        }
    }

    private void SubmitBorrow()
    {
        if (dataProvider == null) return;

        string applicant = borrowApplicantInput != null ? borrowApplicantInput.text.Trim() : "";
        string start = borrowStartInput != null ? borrowStartInput.text.Trim() : "";
        string end = borrowEndInput != null ? borrowEndInput.text.Trim() : "";
        string purpose = borrowPurposeInput != null ? borrowPurposeInput.text.Trim() : "";

        if (string.IsNullOrEmpty(applicant)) { ShowBorrowStatus("请填写申请人姓名", false); return; }
        if (string.IsNullOrEmpty(start))     { ShowBorrowStatus("请填写预计开始时间", false); return; }
        if (string.IsNullOrEmpty(end))       { ShowBorrowStatus("请填写预计归还时间", false); return; }
        if (string.IsNullOrEmpty(purpose))   { ShowBorrowStatus("请填写借用用途", false); return; }

        // 检查设备是否可借
        var devices = dataProvider.GetAllDevices();
        int selIdx = borrowDeviceDropdown != null ? borrowDeviceDropdown.value : 0;
        if (selIdx < devices.Count && devices[selIdx].deviceStatus != "闲置")
        {
            ShowBorrowStatus($"所选设备当前状态为「{devices[selIdx].deviceStatus}」，不可借用", false);
            return;
        }

        if (borrowSubmitButton != null) borrowSubmitButton.interactable = false;
        dataProvider.SubmitDeviceBorrow(new DeviceBorrowRequest
        {
            deviceId = devices[selIdx].id,
            deviceName = devices[selIdx].name,
            applicantName = applicant,
            expectedStartStr = start,
            expectedReturnStr = end,
            purpose = purpose
        }, (success, message) =>
        {
            if (borrowSubmitButton != null) borrowSubmitButton.interactable = true;
            if (borrowSuccessOverlay != null) borrowSuccessOverlay.SetActive(success);
            ShowBorrowStatus(message, success);

            if (success)
            {
                RefreshDeviceList();
                RefreshBorrowRecords();
                if (currentDetailDevice != null)
                    ShowDeviceDetail(dataProvider.GetDeviceById(currentDetailDevice.id));
            }
        });
    }

    private void ClearBorrowForm()
    {
        if (borrowApplicantInput != null) borrowApplicantInput.text = "";
        if (borrowStartInput != null) borrowStartInput.text = "";
        if (borrowEndInput != null) borrowEndInput.text = "";
        if (borrowPurposeInput != null) borrowPurposeInput.text = "";
        if (borrowStatusText != null) { borrowStatusText.text = ""; borrowStatusText.gameObject.SetActive(false); }
        if (borrowSuccessOverlay != null) borrowSuccessOverlay.SetActive(false);
        ApplyBorrowApplicantNameFromContext();
    }

    private void ApplyBorrowApplicantNameFromContext()
    {
        if (borrowApplicantInput == null)
            return;

        string currentUserName = WebGLBridge.CurrentContext != null ? WebGLBridge.CurrentContext.userName : string.Empty;
        if (string.IsNullOrWhiteSpace(currentUserName))
            return;

        borrowApplicantInput.SetTextWithoutNotify(currentUserName);
    }

    private void ShowBorrowStatus(string msg, bool success)
    {
        if (borrowStatusText == null) return;
        borrowStatusText.gameObject.SetActive(true);
        borrowStatusText.text = msg;
        borrowStatusText.color = success ? new Color(0.16f, 0.8f, 0.44f) : new Color(0.91f, 0.3f, 0.24f);
    }

    // ─────────────────────────────────────────────────────────
    //  Tab 2 — 借用记录
    // ─────────────────────────────────────────────────────────

    private void RefreshBorrowRecords()
    {
        if (dataProvider == null || recordListContainer == null || recordRowPrefab == null) return;

        foreach (Transform child in recordListContainer) Destroy(child.gameObject);

        foreach (var record in dataProvider.GetBorrowRecords())
        {
            GameObject row = Instantiate(recordRowPrefab, recordListContainer);
            row.SetActive(true);
            SetText(row, "DeviceNameText", record.deviceName);
            SetText(row, "BorrowerText", record.borrowerName);
            SetText(row, "OutTimeText", record.actualOutTimeStr);
            SetText(row, "ReturnTimeText", record.actualReturnTimeStr);
            SetText(row, "StatusText", record.StatusLabel);

            var statusImg = FindInChildren<Image>(row, "StatusBadge");
            if (statusImg != null && ColorUtility.TryParseHtmlString(record.StatusColor, out Color c))
                statusImg.color = c;

            // 逾期行高亮整行背景
            if (record.status == 3)
            {
                var rowImg = row.GetComponent<Image>();
                if (rowImg != null) rowImg.color = new Color(0.91f, 0.3f, 0.24f, 0.12f);
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Tab 3 — 维修上报
    // ─────────────────────────────────────────────────────────

    private void RefreshRepairForm()
    {
        if (dataProvider == null || repairDeviceDropdown == null) return;

        repairDeviceDropdown.ClearOptions();
        var options = new List<string>();
        foreach (var d in dataProvider.GetAllDevices())
            options.Add($"{d.name}（{d.deviceNo}）");
        repairDeviceDropdown.AddOptions(options);

        if (repairStatusText != null) { repairStatusText.text = ""; repairStatusText.gameObject.SetActive(false); }

        // 若从详情页跳来，预选当前设备
        if (currentDetailDevice != null)
        {
            var all = dataProvider.GetAllDevices();
            int idx = all.FindIndex(d => d.id == currentDetailDevice.id);
            if (idx >= 0) repairDeviceDropdown.value = idx;
        }

        RefreshRepairHistory();
    }

    private void SubmitRepair()
    {
        string desc = repairDescInput != null ? repairDescInput.text.Trim() : "";
        if (string.IsNullOrEmpty(desc)) { ShowRepairStatus("请填写故障描述", false); return; }

        var devices = dataProvider != null ? dataProvider.GetAllDevices() : new List<DeviceData>();
        int selIdx = repairDeviceDropdown != null ? repairDeviceDropdown.value : 0;
        if (selIdx >= devices.Count) { ShowRepairStatus("请选择有效设备", false); return; }

        if (repairSubmitButton != null) repairSubmitButton.interactable = false;
        dataProvider.SubmitRepairRequest(devices[selIdx].id, desc, (success, message) =>
        {
            if (repairSubmitButton != null) repairSubmitButton.interactable = true;
            ShowRepairStatus(message, success);

            if (success)
            {
                if (repairDescInput != null) repairDescInput.text = "";
                RefreshDeviceList();
                RefreshRepairHistory();
                if (currentDetailDevice != null)
                    ShowDeviceDetail(dataProvider.GetDeviceById(currentDetailDevice.id));
            }
        });
    }

    private void ShowRepairStatus(string msg, bool success)
    {
        if (repairStatusText == null) return;
        repairStatusText.gameObject.SetActive(true);
        repairStatusText.text = msg;
        repairStatusText.color = success ? new Color(0.16f, 0.8f, 0.44f) : new Color(0.91f, 0.3f, 0.24f);
    }

    private void RefreshRepairHistory()
    {
        if (dataProvider == null || repairRecordListContainer == null || repairRecordRowPrefab == null)
            return;

        foreach (Transform child in repairRecordListContainer) Destroy(child.gameObject);

        List<DeviceRepairRecord> records = dataProvider.GetRepairRecords();
        if (currentDetailDevice != null)
            records = records.FindAll(r => r.deviceId == currentDetailDevice.id);

        if (repairHistoryPlaceholderText != null)
        {
            repairHistoryPlaceholderText.gameObject.SetActive(records.Count == 0);
            repairHistoryPlaceholderText.text = currentDetailDevice != null ? "当前设备暂无维修历史" : "暂无维修记录";
        }

        foreach (DeviceRepairRecord record in records)
        {
            GameObject row = Instantiate(repairRecordRowPrefab, repairRecordListContainer);
            row.SetActive(true);
            SetText(row, "DeviceNameText", record.deviceName);
            SetText(row, "ReportTimeText", record.reportTimeStr);
            SetText(row, "DescText", record.description);
            SetText(row, "HandlerText", record.handlerName);
            SetText(row, "StatusText", record.repairStatus);

            Image badge = FindInChildren<Image>(row, "StatusBadge");
            if (badge != null && ColorUtility.TryParseHtmlString(record.StatusColor, out Color badgeColor))
                badge.color = badgeColor;
        }
    }

    private void SetupFilterOptions()
    {
        if (dataProvider == null)
            return;

        if (categoryFilterDropdown != null)
        {
            List<string> categories = new List<string> { "全部" };
            foreach (DeviceData device in dataProvider.GetAllDevices())
            {
                if (!categories.Contains(device.category))
                    categories.Add(device.category);
            }

            categoryFilterDropdown.ClearOptions();
            categoryFilterDropdown.AddOptions(categories);
            categoryFilterDropdown.SetValueWithoutNotify(0);
        }

        if (statusFilterDropdown != null)
        {
            List<string> statuses = new List<string> { "全部" };
            foreach (DeviceData device in dataProvider.GetAllDevices())
            {
                if (!statuses.Contains(device.deviceStatus))
                    statuses.Add(device.deviceStatus);
            }

            statusFilterDropdown.ClearOptions();
            statusFilterDropdown.AddOptions(statuses);
            statusFilterDropdown.SetValueWithoutNotify(0);
        }
    }

    private void ApplyResponsiveLayout()
    {
        if (listPanel != null && listPanel.activeInHierarchy)
            ApplyResponsiveGrid(cardContainer, ListCardMinWidth, ListMinColumns, ListMaxColumns);
    }

    private void ApplyResponsiveGrid(Transform container, float minCellWidth, int minColumns, int maxColumns)
    {
        if (container == null)
            return;

        GridLayoutGroup grid = container.GetComponent<GridLayoutGroup>();
        RectTransform viewportRect = container.parent as RectTransform;
        if (grid == null || viewportRect == null)
            return;

        Canvas.ForceUpdateCanvases();

        float availableWidth = viewportRect.rect.width - grid.padding.left - grid.padding.right;
        if (availableWidth <= 0f)
            return;

        float spacingX = grid.spacing.x;
        int calculatedColumns = Mathf.FloorToInt((availableWidth + spacingX) / (minCellWidth + spacingX));
        calculatedColumns = Mathf.Clamp(calculatedColumns, minColumns, maxColumns);

        int childCount = Mathf.Max(1, container.childCount);
        int columnCount = Mathf.Max(1, Mathf.Min(calculatedColumns, childCount));
        float cellWidth = (availableWidth - spacingX * (columnCount - 1)) / columnCount;

        grid.constraint = GridLayoutGroup.Constraint.FixedColumnCount;
        grid.constraintCount = columnCount;
        grid.cellSize = new Vector2(Mathf.Max(minCellWidth, cellWidth), grid.cellSize.y);
        grid.childAlignment = TextAnchor.UpperLeft;
    }

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
