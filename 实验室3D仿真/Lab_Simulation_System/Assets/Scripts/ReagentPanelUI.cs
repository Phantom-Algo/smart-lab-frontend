using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// 药材/试剂管理面板控制器
/// Tab 0 — 试剂列表（卡片网格 + 右侧详情）
/// Tab 1 — 领用记录
/// Tab 2 — 库存预警
/// </summary>
public class ReagentPanelUI : MonoBehaviour
{
    private const float ListCardMinWidth = 240f;
    private const float WarningCardMinWidth = 250f;
    private const int ListMinColumns = 2;
    private const int ListMaxColumns = 3;
    private const int WarningMinColumns = 2;
    private const int WarningMaxColumns = 5;

    [Header("=== 顶部标题栏 ===")]
    public TextMeshProUGUI titleText;
    public Button closeButton;

    [Header("=== Tab 切换 ===")]
    public Button tabListButton;
    public Button tabPickupButton;
    public Button tabWarningButton;
    public GameObject tabListHighlight;
    public GameObject tabPickupHighlight;
    public GameObject tabWarningHighlight;
    public Color tabActiveColor = new Color(0.09f, 0.13f, 0.19f);
    public Color tabInactiveColor = new Color(0.14f, 0.19f, 0.28f);

    [Header("=== Tab 0: 试剂列表 ===")]
    public GameObject listPanel;
    public Transform cardContainer;
    public GameObject cardPrefab;
    public TMP_InputField searchInput;
    public TMP_Dropdown categoryDropdown;
    public TMP_Dropdown statusDropdown;
    public TextMeshProUGUI listSummaryText;

    [Header("=== 右侧详情面板 ===")]
    public GameObject detailPanel;
    public TextMeshProUGUI detailNameText;
    public TextMeshProUGUI detailCasText;
    public TextMeshProUGUI detailSpecText;
    public TextMeshProUGUI detailStockText;
    public TextMeshProUGUI detailLocationText;
    public TextMeshProUGUI detailExpireText;
    public TextMeshProUGUI detailSafetyText;
    public TextMeshProUGUI detailStatusText;
    public Image detailStatusBadge;
    public Transform batchListContainer;
    public GameObject batchRowPrefab;
    public TextMeshProUGUI batchEmptyText;

    [Header("=== 详情操作按钮 ===")]
    public Button pickupApplyButton;
    public Button viewMsdsButton;

    [Header("=== Tab 1: 领用记录 ===")]
    public GameObject pickupPanel;
    public TextMeshProUGUI pickupPlaceholderText;
    public Transform pickupRecordListContainer;
    public GameObject pickupRecordRowPrefab;
    public Button pickupQuickApplyButton;

    [Header("=== Tab 2: 库存预警 ===")]
    public GameObject warningPanel;
    public Transform warningListContainer;

    [Header("=== 领用申请弹窗 ===")]
    public GameObject pickupModal;
    public TextMeshProUGUI pickupModalTitleText;
    public TMP_Dropdown pickupReagentDropdown;
    public TMP_InputField pickupApplicantInput;
    public TMP_InputField pickupQuantityInput;
    public TMP_InputField pickupPurposeInput;
    public Button pickupSubmitButton;
    public Button pickupCancelButton;
    public TextMeshProUGUI pickupStatusText;

    [Header("=== 数据源 ===")]
    public ReagentDataProvider dataProvider;

    private int currentTab = 0;
    private ReagentData currentDetailReagent;
    private List<ReagentData> currentDisplayList = new List<ReagentData>();

    private void Awake()
    {
        if (Application.isPlaying)
            gameObject.SetActive(false);
    }

    private void Start()
    {
        BindEvents();
    }

    private void OnEnable()
    {
        BindEvents();
        EnsureDataProvider();
        SetupFilterOptions();
        SmartLabDropdownRuntimeFix.Normalize(categoryDropdown);
        SmartLabDropdownRuntimeFix.Normalize(statusDropdown);
        SmartLabDropdownRuntimeFix.Normalize(pickupReagentDropdown);
        SwitchTab(currentTab);
        if (pickupModal != null) pickupModal.SetActive(false);
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
        if (tabPickupButton != null) { tabPickupButton.onClick.RemoveAllListeners(); tabPickupButton.onClick.AddListener(() => SwitchTab(1)); }
        if (tabWarningButton != null) { tabWarningButton.onClick.RemoveAllListeners(); tabWarningButton.onClick.AddListener(() => SwitchTab(2)); }
        if (pickupApplyButton != null) { pickupApplyButton.onClick.RemoveAllListeners(); pickupApplyButton.onClick.AddListener(OpenPickupModal); }
        if (pickupQuickApplyButton != null) { pickupQuickApplyButton.onClick.RemoveAllListeners(); pickupQuickApplyButton.onClick.AddListener(OpenPickupModal); }
        if (pickupSubmitButton != null) { pickupSubmitButton.onClick.RemoveAllListeners(); pickupSubmitButton.onClick.AddListener(SubmitPickup); }
        if (pickupCancelButton != null) { pickupCancelButton.onClick.RemoveAllListeners(); pickupCancelButton.onClick.AddListener(ClosePickupModal); }
        if (searchInput != null) { searchInput.onValueChanged.RemoveAllListeners(); searchInput.onValueChanged.AddListener(OnSearchChanged); }
        if (categoryDropdown != null) { categoryDropdown.onValueChanged.RemoveAllListeners(); categoryDropdown.onValueChanged.AddListener(OnCategoryChanged); }
        if (statusDropdown != null) { statusDropdown.onValueChanged.RemoveAllListeners(); statusDropdown.onValueChanged.AddListener(OnStatusChanged); }
        if (viewMsdsButton != null) { viewMsdsButton.onClick.RemoveAllListeners(); viewMsdsButton.onClick.AddListener(OpenMsdsPlaceholder); }
    }

    public void RefreshData()
    {
        EnsureDataProvider();
        SetupFilterOptions();
        RefreshCurrentTab();

        if (currentDetailReagent != null && dataProvider != null)
        {
            ReagentData updated = dataProvider.GetReagentById(currentDetailReagent.id);
            if (updated != null)
                ShowReagentDetail(updated);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Tab 管理
    // ─────────────────────────────────────────────────────────

    private void SwitchTab(int index)
    {
        currentTab = index;
        if (listPanel != null) listPanel.SetActive(index == 0);
        if (pickupPanel != null) pickupPanel.SetActive(index == 1);
        if (warningPanel != null) warningPanel.SetActive(index == 2);

        UpdateTabHighlights(index);
        RefreshCurrentTab();
    }

    private void UpdateTabHighlights(int active)
    {
        UpdateTabStyle(tabListButton, tabListHighlight, active == 0);
        UpdateTabStyle(tabPickupButton, tabPickupHighlight, active == 1);
        UpdateTabStyle(tabWarningButton, tabWarningHighlight, active == 2);
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

    private void RefreshCurrentTab()
    {
        switch (currentTab)
        {
            case 0: RefreshReagentList(); break;
            case 1: RefreshPickupRecords(); break;
            case 2: RefreshWarningList(); break;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Tab 0 — 试剂列表
    // ─────────────────────────────────────────────────────────

    private void RefreshReagentList(string filter = "", string category = "")
    {
        if (dataProvider == null || cardContainer == null || cardPrefab == null) return;

        string currentFilter = searchInput != null ? searchInput.text.Trim() : filter;
        string currentCategory = GetSelectedText(categoryDropdown, category);
        string currentStatus = GetSelectedText(statusDropdown, "");

        var all = dataProvider.GetAllReagents();
        currentDisplayList = new List<ReagentData>();
        foreach (var r in all)
        {
            bool matchFilter = string.IsNullOrEmpty(currentFilter)
                || r.name.IndexOf(currentFilter, System.StringComparison.OrdinalIgnoreCase) >= 0
                || (!string.IsNullOrEmpty(r.casNo) && r.casNo.IndexOf(currentFilter, System.StringComparison.OrdinalIgnoreCase) >= 0);
            bool matchCat = string.IsNullOrEmpty(currentCategory) || currentCategory == "全部" || r.category == currentCategory;
            bool matchStatus = string.IsNullOrEmpty(currentStatus) || currentStatus == "全部" || r.StatusLabel == currentStatus;
            if (matchFilter && matchCat && matchStatus) currentDisplayList.Add(r);
        }

        foreach (Transform child in cardContainer) Destroy(child.gameObject);

        foreach (var reagent in currentDisplayList)
        {
            GameObject card = Instantiate(cardPrefab, cardContainer);
            card.SetActive(true);
            PopulateReagentCard(card, reagent);
        }

        ApplyResponsiveGrid(cardContainer, ListCardMinWidth, ListMinColumns, ListMaxColumns, false);

        if (currentDisplayList.Count > 0)
        {
            ReagentData selected = currentDisplayList.Find(r => currentDetailReagent != null && r.id == currentDetailReagent.id) ?? currentDisplayList[0];
            ShowReagentDetail(selected);
        }
        else if (detailPanel != null)
        {
            detailPanel.SetActive(false);
            currentDetailReagent = null;
        }

        if (listSummaryText != null)
            listSummaryText.text = $"共 {currentDisplayList.Count} 项，低库存 {dataProvider.GetWarningReagents().FindAll(r => r.IsLowStock).Count} 项，临期/过期 {dataProvider.GetWarningReagents().FindAll(r => r.IsExpiringSoon || r.IsExpired).Count} 项";
    }

    private void PopulateReagentCard(GameObject card, ReagentData reagent)
    {
        SetText(card, "NameText", reagent.name);
        SetText(card, "StockText", $"库存：{reagent.currentStock}{reagent.unit}");
        SetText(card, "CategoryText", reagent.category);
        SetText(card, "StatusText", reagent.StatusLabel);
        if (reagent.batches != null && reagent.batches.Count > 0)
            SetText(card, "BatchText", $"批号：{reagent.batches[0].batchNo}");

        // 状态颜色
        var statusImg = FindInChildren<Image>(card, "StatusBadge");
        if (statusImg != null && ColorUtility.TryParseHtmlString(reagent.StatusColor, out Color c))
            statusImg.color = c;

        var btn = card.GetComponentInChildren<Button>();
        if (btn != null)
        {
            btn.onClick.RemoveAllListeners();
            var r = reagent;
            btn.onClick.AddListener(() => ShowReagentDetail(r));
        }
    }

    private void ShowReagentDetail(ReagentData reagent)
    {
        currentDetailReagent = reagent;
        if (detailPanel == null) return;
        detailPanel.SetActive(true);

        if (detailNameText != null) detailNameText.text = reagent.name;
        if (detailCasText != null) detailCasText.text = $"CAS: {reagent.casNo}";
        if (detailSpecText != null) detailSpecText.text = reagent.spec;
        if (detailStockText != null) detailStockText.text = $"{reagent.currentStock} {reagent.unit}  （安全线 {reagent.safetyStock}{reagent.unit}）";
        if (detailLocationText != null) detailLocationText.text = reagent.location;
        if (detailSafetyText != null) detailSafetyText.text = reagent.SafetyLevelLabel;

        if (detailStatusText != null) detailStatusText.text = reagent.StatusLabel;
        if (detailStatusBadge != null && ColorUtility.TryParseHtmlString(reagent.StatusColor, out Color c))
            detailStatusBadge.color = c;

        string expireInfo = reagent.expireDateStr;
        int days = reagent.DaysUntilExpiry;
        if (days < 0)
            expireInfo += " <color=#E74C3C>（已过期）</color>";
        else if (days <= 30)
            expireInfo += $" <color=#F39C12>（剩余 {days} 天）</color>";
        else
            expireInfo += $"（剩余 {days} 天）";
        if (detailExpireText != null) { detailExpireText.text = expireInfo; detailExpireText.richText = true; }

        RefreshBatchList(reagent);
    }

    private void RefreshBatchList(ReagentData reagent)
    {
        if (batchListContainer == null)
            return;

        foreach (Transform child in batchListContainer) Destroy(child.gameObject);

        int batchCount = reagent.batches != null ? reagent.batches.Count : 0;
        if (batchEmptyText != null)
            batchEmptyText.gameObject.SetActive(batchCount == 0);

        if (reagent.batches == null || batchRowPrefab == null)
            return;

        foreach (ReagentBatchData batch in reagent.batches)
        {
            GameObject row = Instantiate(batchRowPrefab, batchListContainer);
            row.SetActive(true);
            SetText(row, "BatchNoText", batch.batchNo);
            SetText(row, "EntryDateText", batch.entryDateStr);
            SetText(row, "RemainText", $"{batch.remainQuantity}/{batch.entryQuantity}{reagent.unit}");
            SetText(row, "StatusText", batch.status);

            Image badge = FindInChildren<Image>(row, "StatusBadge");
            if (badge != null)
            {
                string colorHex = batch.status == "已耗尽" ? "#E74C3C" : batch.status == "低库存" || batch.status == "临期" ? "#F39C12" : "#27AE60";
                if (ColorUtility.TryParseHtmlString(colorHex, out Color badgeColor))
                    badge.color = badgeColor;
            }
        }
    }

    private void OnSearchChanged(string val) => RefreshReagentList(val);
    private void OnCategoryChanged(int idx)
    {
        RefreshReagentList(searchInput != null ? searchInput.text : "");
    }

    private void OnStatusChanged(int idx) => RefreshReagentList(searchInput != null ? searchInput.text : "");

    // ─────────────────────────────────────────────────────────
    //  Tab 1 — 领用记录（占位文字）
    // ─────────────────────────────────────────────────────────

    private void RefreshPickupRecords()
    {
        if (dataProvider == null)
            return;

        if (pickupRecordListContainer != null)
        {
            foreach (Transform child in pickupRecordListContainer) Destroy(child.gameObject);

            List<ReagentPickupRecord> records = dataProvider.GetPickupRecords();
            if (pickupPlaceholderText != null)
            {
                pickupPlaceholderText.gameObject.SetActive(records.Count == 0);
                pickupPlaceholderText.text = records.Count == 0 ? "暂无领用记录" : "最近领用记录";
            }

            if (pickupRecordRowPrefab != null)
            {
                foreach (ReagentPickupRecord record in records)
                {
                    GameObject row = Instantiate(pickupRecordRowPrefab, pickupRecordListContainer);
                    row.SetActive(true);
                    SetText(row, "ApplicantText", record.applicantName);
                    SetText(row, "ReagentText", record.reagentName);
                    SetText(row, "QuantityText", $"{record.quantity}{record.unit}");
                    SetText(row, "PickupTimeText", record.pickupTimeStr);
                    SetText(row, "StatusText", record.status);

                    Image badge = FindInChildren<Image>(row, "StatusBadge");
                    if (badge != null && ColorUtility.TryParseHtmlString(record.StatusColor, out Color badgeColor))
                        badge.color = badgeColor;
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Tab 2 — 库存预警
    // ─────────────────────────────────────────────────────────

    private void RefreshWarningList()
    {
        if (dataProvider == null || warningListContainer == null) return;

        foreach (Transform child in warningListContainer) Destroy(child.gameObject);

        var warnings = dataProvider.GetWarningReagents();
        foreach (var r in warnings)
        {
            if (cardPrefab == null) break;
            GameObject card = Instantiate(cardPrefab, warningListContainer);
            card.SetActive(true);
            PopulateReagentCard(card, r);
        }

        ApplyResponsiveGrid(warningListContainer, WarningCardMinWidth, WarningMinColumns, WarningMaxColumns, true);
    }

    // ─────────────────────────────────────────────────────────
    //  领用申请弹窗
    // ─────────────────────────────────────────────────────────

    private void OpenPickupModal()
    {
        if (pickupModal == null) return;

        if (currentDetailReagent == null && currentDisplayList.Count > 0)
            currentDetailReagent = currentDisplayList[0];

        if (currentDetailReagent == null) return;

        pickupModal.SetActive(true);
        RefreshPickupReagentDropdown();
        if (pickupModalTitleText != null)
            pickupModalTitleText.text = $"申请领用：{currentDetailReagent.name}";
        if (pickupStatusText != null) { pickupStatusText.text = ""; pickupStatusText.gameObject.SetActive(false); }
        ApplyPickupApplicantNameFromContext();
        if (pickupQuantityInput != null) pickupQuantityInput.text = "";
        if (pickupPurposeInput != null) pickupPurposeInput.text = "";
    }

    private void ClosePickupModal()
    {
        if (pickupModal != null) pickupModal.SetActive(false);
    }

    private void SubmitPickup()
    {
        if (currentDetailReagent == null) return;

        string applicant = pickupApplicantInput != null ? pickupApplicantInput.text.Trim() : "";
        string quantityStr = pickupQuantityInput != null ? pickupQuantityInput.text.Trim() : "";
        string purpose = pickupPurposeInput != null ? pickupPurposeInput.text.Trim() : "";

        ReagentData selectedReagent = currentDetailReagent;
        if (pickupReagentDropdown != null && dataProvider != null)
        {
            List<ReagentData> reagents = dataProvider.GetAllReagents();
            int idx = Mathf.Clamp(pickupReagentDropdown.value, 0, reagents.Count - 1);
            if (reagents.Count > 0)
                selectedReagent = reagents[idx];
        }

        if (selectedReagent == null)
        { ShowPickupStatus("请选择要领用的试剂", false); return; }

        if (string.IsNullOrEmpty(applicant))
        { ShowPickupStatus("请填写申请人姓名", false); return; }
        if (!float.TryParse(quantityStr, out float qty) || qty <= 0)
        { ShowPickupStatus("请填写有效的领用数量", false); return; }
        if (string.IsNullOrEmpty(purpose))
        { ShowPickupStatus("请填写领用用途", false); return; }
        if (qty > selectedReagent.currentStock)
        { ShowPickupStatus($"领用数量超出库存（现有 {selectedReagent.currentStock}{selectedReagent.unit}）", false); return; }

        if (dataProvider == null)
        {
            ShowPickupStatus("数据服务未就绪", false);
            return;
        }

        if (pickupSubmitButton != null) pickupSubmitButton.interactable = false;
        dataProvider.SubmitPickupRequest(new ReagentPickupRequest
        {
            reagentId = selectedReagent.id,
            reagentName = selectedReagent.name,
            quantity = qty,
            unit = selectedReagent.unit,
            applicantName = applicant,
            pickupDateStr = System.DateTime.Now.ToString("yyyy-MM-dd HH:mm"),
            purpose = purpose
        }, (success, message) =>
        {
            if (pickupSubmitButton != null) pickupSubmitButton.interactable = true;

            ShowPickupStatus(message, success);

            if (success)
            {
                RefreshData();
                currentDetailReagent = dataProvider.GetReagentById(selectedReagent.id);
                if (currentDetailReagent != null)
                    ShowReagentDetail(currentDetailReagent);
            }
        });
    }

    private void ShowPickupStatus(string msg, bool success)
    {
        if (pickupStatusText == null) return;
        pickupStatusText.gameObject.SetActive(true);
        pickupStatusText.text = msg;
        pickupStatusText.color = success
            ? new Color(0.16f, 0.8f, 0.44f)
            : new Color(0.91f, 0.3f, 0.24f);
    }

    private void OpenMsdsPlaceholder()
    {
        ShowPickupStatus("MSDS 文档查看功能已预留，后续可接入实际文档链接。", true);
    }

    private void SetupFilterOptions()
    {
        if (dataProvider == null)
            return;

        if (categoryDropdown != null)
        {
            List<string> categories = new List<string> { "全部" };
            foreach (ReagentData reagent in dataProvider.GetAllReagents())
            {
                if (!categories.Contains(reagent.category))
                    categories.Add(reagent.category);
            }

            categoryDropdown.ClearOptions();
            categoryDropdown.AddOptions(categories);
            categoryDropdown.SetValueWithoutNotify(0);
        }

        if (statusDropdown != null)
        {
            statusDropdown.ClearOptions();
            statusDropdown.AddOptions(new List<string> { "全部", "正常", "低库存", "即将过期", "已过期" });
            statusDropdown.SetValueWithoutNotify(0);
        }
    }

    private void RefreshPickupReagentDropdown()
    {
        if (pickupReagentDropdown == null || dataProvider == null)
            return;

        List<ReagentData> reagents = dataProvider.GetAllReagents();
        List<string> options = new List<string>();
        int selectedIndex = 0;

        for (int i = 0; i < reagents.Count; i++)
        {
            ReagentData reagent = reagents[i];
            options.Add($"{reagent.name}（库存 {reagent.currentStock}{reagent.unit}）");
            if (currentDetailReagent != null && reagent.id == currentDetailReagent.id)
                selectedIndex = i;
        }

        pickupReagentDropdown.ClearOptions();
        pickupReagentDropdown.AddOptions(options);
        pickupReagentDropdown.SetValueWithoutNotify(selectedIndex);
        pickupReagentDropdown.RefreshShownValue();
    }

    private void ApplyPickupApplicantNameFromContext()
    {
        if (pickupApplicantInput == null)
            return;

        string currentUserName = WebGLBridge.CurrentContext != null ? WebGLBridge.CurrentContext.userName : string.Empty;
        if (string.IsNullOrWhiteSpace(currentUserName))
            return;

        pickupApplicantInput.SetTextWithoutNotify(currentUserName);
    }

    private string GetSelectedText(TMP_Dropdown dropdown, string fallback)
    {
        if (dropdown == null || dropdown.options == null || dropdown.options.Count == 0)
            return fallback;

        int index = Mathf.Clamp(dropdown.value, 0, dropdown.options.Count - 1);
        return dropdown.options[index].text;
    }

    private void ApplyResponsiveLayout()
    {
        if (listPanel != null && listPanel.activeInHierarchy)
            ApplyResponsiveGrid(cardContainer, ListCardMinWidth, ListMinColumns, ListMaxColumns, false);

        if (warningPanel != null && warningPanel.activeInHierarchy)
            ApplyResponsiveGrid(warningListContainer, WarningCardMinWidth, WarningMinColumns, WarningMaxColumns, true);
    }

    private void ApplyResponsiveGrid(Transform container, float minCellWidth, int minColumns, int maxColumns, bool centerIfSparse)
    {
        if (container == null)
            return;

        GridLayoutGroup grid = container.GetComponent<GridLayoutGroup>();
        RectTransform contentRect = container as RectTransform;
        RectTransform viewportRect = container.parent as RectTransform;
        if (grid == null || contentRect == null || viewportRect == null)
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
        grid.childAlignment = centerIfSparse && childCount < columnCount ? TextAnchor.UpperCenter : TextAnchor.UpperLeft;
    }

    // ─────────────────────────────────────────────────────────
    //  关闭面板（通知 ScreenTrigger）
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
