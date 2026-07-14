using System.Collections.Generic;
using TMPro;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

public static class ReagentAndEquipmentUIBuilder
{
    private const string ChineseFontAssetPath = "Assets/字体/ZiHunBingLin.asset";

    private static TMP_FontAsset cachedFont;
    private static Sprite cachedUiSprite;

    private static readonly Color OverlayBg = new Color(0f, 0f, 0f, 0.72f);
    private static readonly Color PanelBg = Hex("#131B27");
    private static readonly Color PanelBgAlt = Hex("#1B2636");
    private static readonly Color PanelBorder = Hex("#314156");
    private static readonly Color CardBg = Hex("#1D2A3A");
    private static readonly Color CardBgAlt = Hex("#243247");
    private static readonly Color RowBg = new Color(1f, 1f, 1f, 0.04f);
    private static readonly Color InputBg = Hex("#0F1622");
    private static readonly Color InputBorder = Hex("#3A4A60");
    private static readonly Color TabActive = Hex("#172131");
    private static readonly Color TabInactive = Hex("#243048");
    private static readonly Color AccentBlue = Hex("#5B9BD5");
    private static readonly Color SuccessGreen = Hex("#27AE60");
    private static readonly Color WarningOrange = Hex("#F39C12");
    private static readonly Color DangerRed = Hex("#E74C3C");
    private static readonly Color TextPrimary = Hex("#F3F5F7");
    private static readonly Color TextSecondary = Hex("#B6BDC9");
    private static readonly Color TextMuted = Hex("#7A8394");
    private static readonly Color Clear = new Color(0f, 0f, 0f, 0f);
    private const float SplitLeftWide = 0.58f;
    private const float SplitRightStart = 0.61f;

    private class PanelShell
    {
        public GameObject root;
        public GameObject overlay;
        public GameObject mainPanel;
        public GameObject tabBar;
        public GameObject bodyRoot;
        public TextMeshProUGUI titleText;
        public Button closeButton;
    }

    [MenuItem("Tools/搭建试剂器材室交互 UI", false, 113)]
    public static void BuildFullUI()
    {
        if (TMP_Settings.instance == null)
        {
            EditorUtility.DisplayDialog(
                "缺少 TextMeshPro",
                "请先通过 Window → TextMeshPro → Import TMP Essential Resources 导入 TMP 资源。",
                "确定");
            return;
        }

        cachedFont = null;
        cachedUiSprite = null;

        EnsureEventSystem();
        EnsureReagentDataProvider();
        EnsureIMEHandler();

        GameObject reagentPanel = BuildReagentPanel();
        GameObject equipmentPanel = BuildEquipmentPanel();
        GameObject monitorPanel = BuildMonitorPanel();
        GameObject promptUI = BuildPromptHUD(out TextMeshProUGUI promptText);
        GameObject triggerRoot = BuildInteractionTriggers(reagentPanel, equipmentPanel, monitorPanel, promptUI, promptText);

        reagentPanel.SetActive(false);
        equipmentPanel.SetActive(false);
        monitorPanel.SetActive(false);
        promptUI.SetActive(false);

        EditorSceneManager.MarkSceneDirty(SceneManager.GetActiveScene());
        Selection.activeGameObject = triggerRoot;

        EditorUtility.DisplayDialog(
            "搭建完成",
            "试剂器材借还室的 3 套交互 UI、默认触发器与共享提示 HUD 已生成。\n\n如需精调，请在场景中移动 ReagentTrigger / EquipmentTrigger / MonitorTrigger 的位置与碰撞范围。",
            "确定");
    }

    private static GameObject BuildReagentPanel()
    {
        PanelShell shell = CreatePanelShell<ReagentPanelUI>(
            "ReagentPanel",
            "药材与试剂管理",
            "覆盖试剂列表、领用记录与库存预警",
            120,
            new Vector2(1500f, 860f),
            out ReagentPanelUI ui);

        ui.titleText = shell.titleText;
        ui.closeButton = shell.closeButton;
        ui.tabActiveColor = TabActive;
        ui.tabInactiveColor = TabInactive;
        ui.dataProvider = FindSceneDataProvider();

        CreateTabButtons(
            shell.tabBar,
            new[] { "试剂列表", "领用记录", "库存预警" },
            out Button[] tabButtons,
            out GameObject[] highlights);

        ui.tabListButton = tabButtons[0];
        ui.tabPickupButton = tabButtons[1];
        ui.tabWarningButton = tabButtons[2];
        ui.tabListHighlight = highlights[0];
        ui.tabPickupHighlight = highlights[1];
        ui.tabWarningHighlight = highlights[2];

        ui.listPanel = CreateElement("ListPanel", shell.bodyRoot);
        StretchFull(ui.listPanel.GetComponent<RectTransform>());
        BuildReagentListPanel(ui.listPanel, ui);

        ui.pickupPanel = CreateElement("PickupPanel", shell.bodyRoot);
        StretchFull(ui.pickupPanel.GetComponent<RectTransform>());
        BuildReagentPickupPanel(ui.pickupPanel, ui);
        ui.pickupPanel.SetActive(false);

        ui.warningPanel = CreateElement("WarningPanel", shell.bodyRoot);
        StretchFull(ui.warningPanel.GetComponent<RectTransform>());
        BuildReagentWarningPanel(ui.warningPanel, ui);
        ui.warningPanel.SetActive(false);

        ui.pickupModal = CreateModalPanel(shell.overlay, "PickupModal", new Vector2(560f, 620f));
        BuildReagentPickupModal(ui.pickupModal, ui);
        ui.pickupModal.SetActive(false);

        GameObject templateRoot = CreateElement("TemplateRoot", shell.root);
        templateRoot.SetActive(false);
        ui.cardPrefab = BuildReagentCardTemplate(templateRoot);
        ui.batchRowPrefab = BuildReagentBatchRowTemplate(templateRoot);
        ui.pickupRecordRowPrefab = BuildPickupRecordRowTemplate(templateRoot);

        EditorUtility.SetDirty(ui);
        EditorUtility.SetDirty(shell.root);
        return shell.root;
    }

    private static GameObject BuildEquipmentPanel()
    {
        PanelShell shell = CreatePanelShell<EquipmentPanelUI>(
            "EquipmentPanel",
            "实验器材管理",
            "覆盖器材列表、借用申请、借用记录与维修上报",
            121,
            new Vector2(1500f, 860f),
            out EquipmentPanelUI ui);

        ui.titleText = shell.titleText;
        ui.closeButton = shell.closeButton;
        ui.tabActiveColor = TabActive;
        ui.tabInactiveColor = TabInactive;
        ui.dataProvider = FindSceneDataProvider();

        CreateTabButtons(
            shell.tabBar,
            new[] { "器材列表", "借用申请", "借用记录", "维修上报" },
            out Button[] tabButtons,
            out GameObject[] highlights);

        ui.tabListButton = tabButtons[0];
        ui.tabBorrowButton = tabButtons[1];
        ui.tabRecordButton = tabButtons[2];
        ui.tabRepairButton = tabButtons[3];
        ui.tabListHighlight = highlights[0];
        ui.tabBorrowHighlight = highlights[1];
        ui.tabRecordHighlight = highlights[2];
        ui.tabRepairHighlight = highlights[3];

        ui.listPanel = CreateElement("ListPanel", shell.bodyRoot);
        StretchFull(ui.listPanel.GetComponent<RectTransform>());
        BuildEquipmentListPanel(ui.listPanel, ui);

        ui.borrowPanel = CreateElement("BorrowPanel", shell.bodyRoot);
        StretchFull(ui.borrowPanel.GetComponent<RectTransform>());
        BuildEquipmentBorrowPanel(ui.borrowPanel, ui);
        ui.borrowPanel.SetActive(false);

        ui.recordPanel = CreateElement("RecordPanel", shell.bodyRoot);
        StretchFull(ui.recordPanel.GetComponent<RectTransform>());
        BuildEquipmentRecordPanel(ui.recordPanel, ui);
        ui.recordPanel.SetActive(false);

        ui.repairPanel = CreateElement("RepairPanel", shell.bodyRoot);
        StretchFull(ui.repairPanel.GetComponent<RectTransform>());
        BuildEquipmentRepairPanel(ui.repairPanel, ui);
        ui.repairPanel.SetActive(false);

        GameObject templateRoot = CreateElement("TemplateRoot", shell.root);
        templateRoot.SetActive(false);
        ui.cardPrefab = BuildEquipmentCardTemplate(templateRoot);
        ui.recordRowPrefab = BuildBorrowRecordRowTemplate(templateRoot);
        ui.repairRecordRowPrefab = BuildRepairRecordRowTemplate(templateRoot);

        EditorUtility.SetDirty(ui);
        EditorUtility.SetDirty(shell.root);
        return shell.root;
    }

    private static GameObject BuildMonitorPanel()
    {
        PanelShell shell = CreatePanelShell<MonitorPanelUI>(
            "MonitorPanel",
            "实验室设备实时监控",
            "覆盖环境概览、设备状态与告警记录",
            122,
            new Vector2(1500f, 860f),
            out MonitorPanelUI ui);

        ui.titleText = shell.titleText;
        ui.closeButton = shell.closeButton;
        ui.tabActiveColor = TabActive;
        ui.tabInactiveColor = TabInactive;
        ui.dataProvider = FindSceneDataProvider();

        ui.lastUpdateText = CreateText(
            "LastUpdateText",
            shell.mainPanel,
            "最后更新：--:--:--",
            12f,
            FontWeight.Regular,
            TextSecondary,
            TextAlignmentOptions.Right);
        RectTransform updateRt = ui.lastUpdateText.GetComponent<RectTransform>();
        updateRt.anchorMin = new Vector2(1f, 1f);
        updateRt.anchorMax = new Vector2(1f, 1f);
        updateRt.pivot = new Vector2(1f, 1f);
        updateRt.sizeDelta = new Vector2(220f, 24f);
        updateRt.anchoredPosition = new Vector2(-78f, -44f);

        CreateTabButtons(
            shell.tabBar,
            new[] { "环境概览", "设备状态", "告警记录" },
            out Button[] tabButtons,
            out GameObject[] highlights);

        ui.tabOverviewButton = tabButtons[0];
        ui.tabDeviceButton = tabButtons[1];
        ui.tabAlertButton = tabButtons[2];
        ui.tabOverviewHighlight = highlights[0];
        ui.tabDeviceHighlight = highlights[1];
        ui.tabAlertHighlight = highlights[2];

        ui.overviewPanel = CreateElement("OverviewPanel", shell.bodyRoot);
        StretchFull(ui.overviewPanel.GetComponent<RectTransform>());
        BuildMonitorOverviewPanel(ui.overviewPanel, ui);

        ui.devicePanel = CreateElement("DevicePanel", shell.bodyRoot);
        StretchFull(ui.devicePanel.GetComponent<RectTransform>());
        BuildMonitorDevicePanel(ui.devicePanel, ui);
        ui.devicePanel.SetActive(false);

        ui.alertPanel = CreateElement("AlertPanel", shell.bodyRoot);
        StretchFull(ui.alertPanel.GetComponent<RectTransform>());
        BuildMonitorAlertPanel(ui.alertPanel, ui);
        ui.alertPanel.SetActive(false);

        GameObject templateRoot = CreateElement("TemplateRoot", shell.root);
        templateRoot.SetActive(false);
        GameObject deviceRowTemplate = BuildMonitorDeviceRowTemplate(templateRoot);
        GameObject alertRowTemplate = BuildAlertRowTemplate(templateRoot);
        ui.deviceRowPrefab = deviceRowTemplate;
        ui.deviceDetailRowPrefab = deviceRowTemplate;
        ui.overviewAlertRowPrefab = alertRowTemplate;
        ui.alertRowPrefab = alertRowTemplate;

        EditorUtility.SetDirty(ui);
        EditorUtility.SetDirty(shell.root);
        return shell.root;
    }

    private static void BuildReagentListPanel(GameObject panel, ReagentPanelUI ui)
    {
        GameObject leftArea = CreateElement("LeftArea", panel);
        SetRect(leftArea.GetComponent<RectTransform>(), new Vector2(0f, 0f), new Vector2(SplitLeftWide, 1f), new Vector2(0f, 1f), Vector2.zero, Vector2.zero);

        GameObject filterCard = CreatePanelCard("FilterCard", leftArea, PanelBgAlt, PanelBorder);
        SetRect(filterCard.GetComponent<RectTransform>(), new Vector2(0f, 1f), new Vector2(1f, 1f), new Vector2(0.5f, 1f), new Vector2(0f, 108f), new Vector2(0f, 0f));

        CreateText("SectionTitleText", filterCard, "试剂列表", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left).GetComponent<RectTransform>().anchoredPosition = Vector2.zero;
        RectTransform sectionTitleRt = filterCard.transform.Find("SectionTitleText").GetComponent<RectTransform>();
        sectionTitleRt.anchorMin = new Vector2(0f, 1f);
        sectionTitleRt.anchorMax = new Vector2(0f, 1f);
        sectionTitleRt.pivot = new Vector2(0f, 1f);
        sectionTitleRt.sizeDelta = new Vector2(180f, 28f);
        sectionTitleRt.anchoredPosition = new Vector2(20f, -14f);

        ui.listSummaryText = CreateText("ListSummaryText", filterCard, "支持按名称、类型与状态筛选", 12f, FontWeight.Regular, TextSecondary, TextAlignmentOptions.Left);
        RectTransform summaryRt = ui.listSummaryText.GetComponent<RectTransform>();
        summaryRt.anchorMin = new Vector2(0f, 1f);
        summaryRt.anchorMax = new Vector2(1f, 1f);
        summaryRt.pivot = new Vector2(0f, 1f);
        summaryRt.sizeDelta = new Vector2(0f, 20f);
        summaryRt.anchoredPosition = new Vector2(20f, -42f);
        summaryRt.offsetMax = new Vector2(-20f, 0f);

        ui.searchInput = CreateCompactInputField(filterCard, "SearchInput", "搜索试剂名称 / CAS", new Vector2(280f, 38f));
        RectTransform searchRt = ui.searchInput.GetComponent<RectTransform>();
        searchRt.anchorMin = new Vector2(0f, 0f);
        searchRt.anchorMax = new Vector2(0f, 0f);
        searchRt.pivot = new Vector2(0f, 0f);
        searchRt.anchoredPosition = new Vector2(20f, 16f);

        ui.categoryDropdown = CreateCompactDropdown(filterCard, "CategoryDropdown", new List<string> { "全部" }, new Vector2(150f, 38f));
        RectTransform categoryRt = ui.categoryDropdown.GetComponent<RectTransform>();
        categoryRt.anchorMin = new Vector2(0f, 0f);
        categoryRt.anchorMax = new Vector2(0f, 0f);
        categoryRt.pivot = new Vector2(0f, 0f);
        categoryRt.anchoredPosition = new Vector2(320f, 16f);

        ui.statusDropdown = CreateCompactDropdown(filterCard, "StatusDropdown", new List<string> { "全部" }, new Vector2(150f, 38f));
        RectTransform statusRt = ui.statusDropdown.GetComponent<RectTransform>();
        statusRt.anchorMin = new Vector2(0f, 0f);
        statusRt.anchorMax = new Vector2(0f, 0f);
        statusRt.pivot = new Vector2(0f, 0f);
        statusRt.anchoredPosition = new Vector2(482f, 16f);

        RectTransform cardContent = CreateGridScrollArea(leftArea, "CardScrollView", new Vector2(0f, 0f), new Vector2(1f, 1f), new Vector2(0f, 0f), new Vector2(0f, -120f), new Vector2(246f, 124f), new Vector2(16f, 16f), 3);
        ui.cardContainer = cardContent;

        ui.detailPanel = CreatePanelCard("DetailPanel", panel, PanelBgAlt, PanelBorder);
        SetRect(ui.detailPanel.GetComponent<RectTransform>(), new Vector2(SplitRightStart, 0f), new Vector2(1f, 1f), new Vector2(1f, 1f), Vector2.zero, Vector2.zero);
        BuildReagentDetailPanel(ui.detailPanel, ui);
    }

    private static void BuildReagentDetailPanel(GameObject detailPanel, ReagentPanelUI ui)
    {
        ui.detailNameText = CreateText("DetailNameText", detailPanel, "请选择左侧试剂", 22f, FontWeight.Bold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform nameRt = ui.detailNameText.GetComponent<RectTransform>();
        nameRt.anchorMin = new Vector2(0f, 1f);
        nameRt.anchorMax = new Vector2(0.78f, 1f);
        nameRt.pivot = new Vector2(0f, 1f);
        nameRt.sizeDelta = new Vector2(0f, 30f);
        nameRt.anchoredPosition = new Vector2(24f, -22f);

        GameObject statusPill = CreatePanelCard("DetailStatusPill", detailPanel, new Color(0.15f, 0.68f, 0.38f, 0.14f), Clear);
        SetRect(statusPill.GetComponent<RectTransform>(), new Vector2(1f, 1f), new Vector2(1f, 1f), new Vector2(1f, 1f), new Vector2(120f, 34f), new Vector2(-22f, -20f));
        ui.detailStatusBadge = statusPill.GetComponent<Image>();
        ui.detailStatusText = CreateText("DetailStatusText", statusPill, "正常", 13f, FontWeight.Medium, TextPrimary, TextAlignmentOptions.Center);
        StretchFull(ui.detailStatusText.GetComponent<RectTransform>());

        GameObject infoGroup = CreateElement("InfoGroup", detailPanel);
        RectTransform infoRt = infoGroup.GetComponent<RectTransform>();
        infoRt.anchorMin = new Vector2(0f, 1f);
        infoRt.anchorMax = new Vector2(1f, 1f);
        infoRt.pivot = new Vector2(0.5f, 1f);
        infoRt.sizeDelta = new Vector2(-48f, 250f);
        infoRt.anchoredPosition = new Vector2(0f, -76f);

        VerticalLayoutGroup infoLayout = infoGroup.AddComponent<VerticalLayoutGroup>();
        infoLayout.spacing = 10f;
        infoLayout.padding = new RectOffset(24, 24, 0, 0);
        infoLayout.childControlWidth = true;
        infoLayout.childControlHeight = true;
        infoLayout.childForceExpandHeight = false;
        infoLayout.childForceExpandWidth = true;

        ui.detailCasText = CreateInfoRow(infoGroup, "DetailCasText", "CAS编号", "-");
        ui.detailSpecText = CreateInfoRow(infoGroup, "DetailSpecText", "规格", "-");
        ui.detailStockText = CreateInfoRow(infoGroup, "DetailStockText", "当前库存", "-");
        ui.detailLocationText = CreateInfoRow(infoGroup, "DetailLocationText", "存放位置", "-");
        ui.detailExpireText = CreateInfoRow(infoGroup, "DetailExpireText", "保质期", "-");
        ui.detailSafetyText = CreateInfoRow(infoGroup, "DetailSafetyText", "安全等级", "-");

        TextMeshProUGUI batchTitle = CreateText("BatchTitleText", detailPanel, "批次列表", 16f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform batchTitleRt = batchTitle.GetComponent<RectTransform>();
        batchTitleRt.anchorMin = new Vector2(0f, 1f);
        batchTitleRt.anchorMax = new Vector2(1f, 1f);
        batchTitleRt.pivot = new Vector2(0f, 1f);
        batchTitleRt.sizeDelta = new Vector2(0f, 24f);
        batchTitleRt.anchoredPosition = new Vector2(24f, -332f);

        RectTransform batchContent = CreateVerticalScrollArea(detailPanel, "BatchScrollView", new Vector2(0f, 0f), new Vector2(1f, 1f), new Vector2(20f, 96f), new Vector2(-20f, -366f), 8f);
        ui.batchListContainer = batchContent;

        ui.batchEmptyText = CreateText("BatchEmptyText", detailPanel, "暂无批次信息", 12f, FontWeight.Regular, TextMuted, TextAlignmentOptions.Center);
        RectTransform batchEmptyRt = ui.batchEmptyText.GetComponent<RectTransform>();
        batchEmptyRt.anchorMin = new Vector2(0f, 0f);
        batchEmptyRt.anchorMax = new Vector2(1f, 0f);
        batchEmptyRt.pivot = new Vector2(0.5f, 0f);
        batchEmptyRt.sizeDelta = new Vector2(0f, 18f);
        batchEmptyRt.anchoredPosition = new Vector2(0f, 126f);

        ui.pickupApplyButton = CreateButton("PickupApplyButton", detailPanel, "申请领用", 13f, FontWeight.SemiBold, TextPrimary, AccentBlue, new Vector2(128f, 40f));
        RectTransform applyRt = ui.pickupApplyButton.GetComponent<RectTransform>();
        applyRt.anchorMin = new Vector2(0f, 0f);
        applyRt.anchorMax = new Vector2(0f, 0f);
        applyRt.pivot = new Vector2(0f, 0f);
        applyRt.anchoredPosition = new Vector2(24f, 26f);

        ui.viewMsdsButton = CreateButton("ViewMsdsButton", detailPanel, "查看 MSDS", 13f, FontWeight.Medium, TextPrimary, TabInactive, new Vector2(128f, 40f));
        RectTransform msdsRt = ui.viewMsdsButton.GetComponent<RectTransform>();
        msdsRt.anchorMin = new Vector2(0f, 0f);
        msdsRt.anchorMax = new Vector2(0f, 0f);
        msdsRt.pivot = new Vector2(0f, 0f);
        msdsRt.anchoredPosition = new Vector2(164f, 26f);
    }

    private static void BuildReagentPickupPanel(GameObject panel, ReagentPanelUI ui)
    {
        GameObject headerCard = CreatePanelCard("HeaderCard", panel, PanelBgAlt, PanelBorder);
        SetRect(headerCard.GetComponent<RectTransform>(), new Vector2(0f, 1f), new Vector2(1f, 1f), new Vector2(0.5f, 1f), new Vector2(0f, 88f), new Vector2(0f, 0f));

        ui.pickupPlaceholderText = CreateText("PickupPlaceholderText", headerCard, "最近领用记录", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform pickupTitleRt = ui.pickupPlaceholderText.GetComponent<RectTransform>();
        pickupTitleRt.anchorMin = new Vector2(0f, 1f);
        pickupTitleRt.anchorMax = new Vector2(0.7f, 1f);
        pickupTitleRt.pivot = new Vector2(0f, 1f);
        pickupTitleRt.sizeDelta = new Vector2(0f, 26f);
        pickupTitleRt.anchoredPosition = new Vector2(20f, -18f);

        TextMeshProUGUI hintText = CreateText("HintText", headerCard, "支持从记录页直接发起新的领用申请", 12f, FontWeight.Regular, TextSecondary, TextAlignmentOptions.Left);
        RectTransform hintRt = hintText.GetComponent<RectTransform>();
        hintRt.anchorMin = new Vector2(0f, 1f);
        hintRt.anchorMax = new Vector2(0.7f, 1f);
        hintRt.pivot = new Vector2(0f, 1f);
        hintRt.sizeDelta = new Vector2(0f, 18f);
        hintRt.anchoredPosition = new Vector2(20f, -46f);

        ui.pickupQuickApplyButton = CreateButton("PickupQuickApplyButton", headerCard, "我要领用", 13f, FontWeight.SemiBold, TextPrimary, AccentBlue, new Vector2(120f, 38f));
        RectTransform quickApplyRt = ui.pickupQuickApplyButton.GetComponent<RectTransform>();
        quickApplyRt.anchorMin = new Vector2(1f, 0.5f);
        quickApplyRt.anchorMax = new Vector2(1f, 0.5f);
        quickApplyRt.pivot = new Vector2(1f, 0.5f);
        quickApplyRt.anchoredPosition = new Vector2(-20f, -4f);

        CreateTableHeader(panel, new[]
        {
            new HeaderColumn("申请人", 120f),
            new HeaderColumn("试剂名称", 180f),
            new HeaderColumn("领用量", 100f),
            new HeaderColumn("领用时间", 180f),
            new HeaderColumn("状态", 120f)
        }, new Vector2(0f, 1f), new Vector2(1f, 1f), new Vector2(0f, -100f), 42f);

        RectTransform recordContent = CreateVerticalScrollArea(panel, "PickupRecordScrollView", new Vector2(0f, 0f), new Vector2(1f, 1f), new Vector2(0f, 0f), new Vector2(0f, -148f), 10f);
        ui.pickupRecordListContainer = recordContent;
    }

    private static void BuildReagentWarningPanel(GameObject panel, ReagentPanelUI ui)
    {
        GameObject warningCard = CreatePanelCard("WarningCard", panel, PanelBgAlt, PanelBorder);
        StretchFull(warningCard.GetComponent<RectTransform>());

        TextMeshProUGUI title = CreateText("WarningTitleText", warningCard, "库存预警", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform titleRt = title.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(0.5f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 24f);
        titleRt.anchoredPosition = new Vector2(20f, -18f);

        TextMeshProUGUI hint = CreateText("WarningHintText", warningCard, "红色表示已过期，橙色表示低库存或临近过期", 12f, FontWeight.Regular, TextSecondary, TextAlignmentOptions.Left);
        RectTransform hintRt = hint.GetComponent<RectTransform>();
        hintRt.anchorMin = new Vector2(0f, 1f);
        hintRt.anchorMax = new Vector2(0.8f, 1f);
        hintRt.pivot = new Vector2(0f, 1f);
        hintRt.sizeDelta = new Vector2(0f, 18f);
        hintRt.anchoredPosition = new Vector2(20f, -46f);

        RectTransform warningContent = CreateGridScrollArea(warningCard, "WarningScrollView", new Vector2(0f, 0f), new Vector2(1f, 1f), new Vector2(20f, 20f), new Vector2(-20f, -80f), new Vector2(246f, 124f), new Vector2(16f, 16f), 4);
        ui.warningListContainer = warningContent;
    }

    private static void BuildReagentPickupModal(GameObject modal, ReagentPanelUI ui)
    {
        ui.pickupModalTitleText = CreateText("PickupModalTitleText", modal, "申请领用", 20f, FontWeight.Bold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform titleRt = ui.pickupModalTitleText.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(1f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 28f);
        titleRt.anchoredPosition = new Vector2(24f, -22f);

        GameObject formRoot = CreateElement("FormRoot", modal);
        RectTransform formRt = formRoot.GetComponent<RectTransform>();
        formRt.anchorMin = new Vector2(0f, 0f);
        formRt.anchorMax = new Vector2(1f, 1f);
        formRt.offsetMin = new Vector2(24f, 96f);
        formRt.offsetMax = new Vector2(-24f, -70f);

        VerticalLayoutGroup formLayout = formRoot.AddComponent<VerticalLayoutGroup>();
        formLayout.spacing = 12f;
        formLayout.childControlWidth = true;
        formLayout.childControlHeight = true;
        formLayout.childForceExpandWidth = true;
        formLayout.childForceExpandHeight = false;

        ui.pickupReagentDropdown = CreateLabeledDropdownField(formRoot, "PickupReagentDropdown", "选择试剂", new List<string> { "全部试剂" }, 74f);
        ui.pickupApplicantInput = CreateLabeledInputField(formRoot, "PickupApplicantInput", "申请人姓名", "请输入申请人姓名", 74f, false);
        ui.pickupQuantityInput = CreateLabeledInputField(formRoot, "PickupQuantityInput", "领用数量", "请输入领用数量", 74f, false);
        ui.pickupPurposeInput = CreateLabeledInputField(formRoot, "PickupPurposeInput", "用途说明", "请输入领用用途", 132f, true);

        ui.pickupStatusText = CreateText("PickupStatusText", modal, "", 12f, FontWeight.Regular, WarningOrange, TextAlignmentOptions.Left);
        RectTransform statusRt = ui.pickupStatusText.GetComponent<RectTransform>();
        statusRt.anchorMin = new Vector2(0f, 0f);
        statusRt.anchorMax = new Vector2(1f, 0f);
        statusRt.pivot = new Vector2(0f, 0f);
        statusRt.sizeDelta = new Vector2(-48f, 18f);
        statusRt.anchoredPosition = new Vector2(24f, 62f);
        ui.pickupStatusText.gameObject.SetActive(false);

        ui.pickupCancelButton = CreateButton("PickupCancelButton", modal, "取消", 13f, FontWeight.Medium, TextPrimary, TabInactive, new Vector2(112f, 40f));
        RectTransform cancelRt = ui.pickupCancelButton.GetComponent<RectTransform>();
        cancelRt.anchorMin = new Vector2(1f, 0f);
        cancelRt.anchorMax = new Vector2(1f, 0f);
        cancelRt.pivot = new Vector2(1f, 0f);
        cancelRt.anchoredPosition = new Vector2(-148f, 18f);

        ui.pickupSubmitButton = CreateButton("PickupSubmitButton", modal, "提交申请", 13f, FontWeight.SemiBold, TextPrimary, AccentBlue, new Vector2(112f, 40f));
        RectTransform submitRt = ui.pickupSubmitButton.GetComponent<RectTransform>();
        submitRt.anchorMin = new Vector2(1f, 0f);
        submitRt.anchorMax = new Vector2(1f, 0f);
        submitRt.pivot = new Vector2(1f, 0f);
        submitRt.anchoredPosition = new Vector2(-24f, 18f);
    }

    private static void BuildEquipmentListPanel(GameObject panel, EquipmentPanelUI ui)
    {
        GameObject leftArea = CreateElement("LeftArea", panel);
        SetRect(leftArea.GetComponent<RectTransform>(), new Vector2(0f, 0f), new Vector2(SplitLeftWide, 1f), new Vector2(0f, 1f), Vector2.zero, Vector2.zero);

        GameObject filterCard = CreatePanelCard("FilterCard", leftArea, PanelBgAlt, PanelBorder);
        SetRect(filterCard.GetComponent<RectTransform>(), new Vector2(0f, 1f), new Vector2(1f, 1f), new Vector2(0.5f, 1f), new Vector2(0f, 108f), new Vector2(0f, 0f));

        TextMeshProUGUI title = CreateText("SectionTitleText", filterCard, "器材列表", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform titleRt = title.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(0f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(160f, 28f);
        titleRt.anchoredPosition = new Vector2(20f, -14f);

        TextMeshProUGUI hint = CreateText("HintText", filterCard, "支持按关键字、器材类型与当前状态筛选", 12f, FontWeight.Regular, TextSecondary, TextAlignmentOptions.Left);
        RectTransform hintRt = hint.GetComponent<RectTransform>();
        hintRt.anchorMin = new Vector2(0f, 1f);
        hintRt.anchorMax = new Vector2(1f, 1f);
        hintRt.pivot = new Vector2(0f, 1f);
        hintRt.sizeDelta = new Vector2(0f, 18f);
        hintRt.anchoredPosition = new Vector2(20f, -42f);
        hintRt.offsetMax = new Vector2(-20f, 0f);

        ui.searchInput = CreateCompactInputField(filterCard, "SearchInput", "搜索设备名称 / 编号", new Vector2(280f, 38f));
        RectTransform searchRt = ui.searchInput.GetComponent<RectTransform>();
        searchRt.anchorMin = new Vector2(0f, 0f);
        searchRt.anchorMax = new Vector2(0f, 0f);
        searchRt.pivot = new Vector2(0f, 0f);
        searchRt.anchoredPosition = new Vector2(20f, 16f);

        ui.categoryFilterDropdown = CreateCompactDropdown(filterCard, "CategoryFilterDropdown", new List<string> { "全部" }, new Vector2(150f, 38f));
        RectTransform catRt = ui.categoryFilterDropdown.GetComponent<RectTransform>();
        catRt.anchorMin = new Vector2(0f, 0f);
        catRt.anchorMax = new Vector2(0f, 0f);
        catRt.pivot = new Vector2(0f, 0f);
        catRt.anchoredPosition = new Vector2(320f, 16f);

        ui.statusFilterDropdown = CreateCompactDropdown(filterCard, "StatusFilterDropdown", new List<string> { "全部" }, new Vector2(150f, 38f));
        RectTransform statusRt = ui.statusFilterDropdown.GetComponent<RectTransform>();
        statusRt.anchorMin = new Vector2(0f, 0f);
        statusRt.anchorMax = new Vector2(0f, 0f);
        statusRt.pivot = new Vector2(0f, 0f);
        statusRt.anchoredPosition = new Vector2(482f, 16f);

        RectTransform cardContent = CreateGridScrollArea(leftArea, "DeviceCardScrollView", new Vector2(0f, 0f), new Vector2(1f, 1f), new Vector2(0f, 0f), new Vector2(0f, -120f), new Vector2(246f, 132f), new Vector2(16f, 16f), 3);
        ui.cardContainer = cardContent;

        ui.detailPanel = CreatePanelCard("DetailPanel", panel, PanelBgAlt, PanelBorder);
        SetRect(ui.detailPanel.GetComponent<RectTransform>(), new Vector2(SplitRightStart, 0f), new Vector2(1f, 1f), new Vector2(1f, 1f), Vector2.zero, Vector2.zero);
        BuildEquipmentDetailPanel(ui.detailPanel, ui);
    }

    private static void BuildEquipmentDetailPanel(GameObject detailPanel, EquipmentPanelUI ui)
    {
        ui.detailNameText = CreateText("DetailNameText", detailPanel, "请选择左侧器材", 22f, FontWeight.Bold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform nameRt = ui.detailNameText.GetComponent<RectTransform>();
        nameRt.anchorMin = new Vector2(0f, 1f);
        nameRt.anchorMax = new Vector2(0.8f, 1f);
        nameRt.pivot = new Vector2(0f, 1f);
        nameRt.sizeDelta = new Vector2(0f, 30f);
        nameRt.anchoredPosition = new Vector2(24f, -22f);

        GameObject statusPill = CreatePanelCard("DetailStatusPill", detailPanel, new Color(0.15f, 0.68f, 0.38f, 0.14f), Clear);
        SetRect(statusPill.GetComponent<RectTransform>(), new Vector2(1f, 1f), new Vector2(1f, 1f), new Vector2(1f, 1f), new Vector2(132f, 34f), new Vector2(-22f, -20f));
        ui.detailStatusBadge = statusPill.GetComponent<Image>();
        ui.detailStatusText = CreateText("DetailStatusText", statusPill, "闲置", 13f, FontWeight.Medium, TextPrimary, TextAlignmentOptions.Center);
        StretchFull(ui.detailStatusText.GetComponent<RectTransform>());

        GameObject infoGroup = CreateElement("InfoGroup", detailPanel);
        RectTransform infoRt = infoGroup.GetComponent<RectTransform>();
        infoRt.anchorMin = new Vector2(0f, 1f);
        infoRt.anchorMax = new Vector2(1f, 1f);
        infoRt.pivot = new Vector2(0.5f, 1f);
        infoRt.sizeDelta = new Vector2(-48f, 230f);
        infoRt.anchoredPosition = new Vector2(0f, -76f);

        VerticalLayoutGroup infoLayout = infoGroup.AddComponent<VerticalLayoutGroup>();
        infoLayout.spacing = 10f;
        infoLayout.padding = new RectOffset(24, 24, 0, 0);
        infoLayout.childControlWidth = true;
        infoLayout.childControlHeight = true;
        infoLayout.childForceExpandHeight = false;
        infoLayout.childForceExpandWidth = true;

        ui.detailNoText = CreateInfoRow(infoGroup, "DetailNoText", "设备编号", "-");
        ui.detailSpecText = CreateInfoRow(infoGroup, "DetailSpecText", "规格参数", "-");
        ui.detailLabText = CreateInfoRow(infoGroup, "DetailLabText", "所属实验室", "-");
        ui.detailPurchaseDateText = CreateInfoRow(infoGroup, "DetailPurchaseDateText", "购入日期", "-");
        ui.detailWarrantyText = CreateInfoRow(infoGroup, "DetailWarrantyText", "保修到期", "-");

        TextMeshProUGUI healthLabel = CreateText("HealthLabelText", detailPanel, "健康评分", 16f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform healthLabelRt = healthLabel.GetComponent<RectTransform>();
        healthLabelRt.anchorMin = new Vector2(0f, 1f);
        healthLabelRt.anchorMax = new Vector2(1f, 1f);
        healthLabelRt.pivot = new Vector2(0f, 1f);
        healthLabelRt.sizeDelta = new Vector2(0f, 24f);
        healthLabelRt.anchoredPosition = new Vector2(24f, -318f);

        ui.detailHealthSlider = CreateSlider(detailPanel, "HealthSlider", new Vector2(280f, 16f), SuccessGreen);
        RectTransform sliderRt = ui.detailHealthSlider.GetComponent<RectTransform>();
        sliderRt.anchorMin = new Vector2(0f, 1f);
        sliderRt.anchorMax = new Vector2(0f, 1f);
        sliderRt.pivot = new Vector2(0f, 1f);
        sliderRt.anchoredPosition = new Vector2(24f, -352f);

        ui.detailHealthText = CreateText("DetailHealthText", detailPanel, "92/100 · 良好", 12f, FontWeight.Regular, TextSecondary, TextAlignmentOptions.Left);
        RectTransform healthTextRt = ui.detailHealthText.GetComponent<RectTransform>();
        healthTextRt.anchorMin = new Vector2(0f, 1f);
        healthTextRt.anchorMax = new Vector2(1f, 1f);
        healthTextRt.pivot = new Vector2(0f, 1f);
        healthTextRt.sizeDelta = new Vector2(0f, 18f);
        healthTextRt.anchoredPosition = new Vector2(24f, -374f);

        ui.detailSensorText = CreateText("DetailSensorText", detailPanel, "温度 -- / 湿度 -- / 电量 -- / 信号 --", 13f, FontWeight.Regular, TextPrimary, TextAlignmentOptions.Left);
        RectTransform sensorRt = ui.detailSensorText.GetComponent<RectTransform>();
        sensorRt.anchorMin = new Vector2(0f, 1f);
        sensorRt.anchorMax = new Vector2(1f, 1f);
        sensorRt.pivot = new Vector2(0f, 1f);
        sensorRt.sizeDelta = new Vector2(0f, 20f);
        sensorRt.anchoredPosition = new Vector2(24f, -410f);

        ui.detailBorrowButton = CreateButton("DetailBorrowButton", detailPanel, "申请借用", 13f, FontWeight.SemiBold, TextPrimary, AccentBlue, new Vector2(108f, 40f));
        RectTransform borrowRt = ui.detailBorrowButton.GetComponent<RectTransform>();
        borrowRt.anchorMin = new Vector2(0f, 0f);
        borrowRt.anchorMax = new Vector2(0f, 0f);
        borrowRt.pivot = new Vector2(0f, 0f);
        borrowRt.anchoredPosition = new Vector2(24f, 24f);

        ui.detailRepairButton = CreateButton("DetailRepairButton", detailPanel, "故障报修", 13f, FontWeight.Medium, TextPrimary, WarningOrange, new Vector2(108f, 40f));
        RectTransform repairRt = ui.detailRepairButton.GetComponent<RectTransform>();
        repairRt.anchorMin = new Vector2(0f, 0f);
        repairRt.anchorMax = new Vector2(0f, 0f);
        repairRt.pivot = new Vector2(0f, 0f);
        repairRt.anchoredPosition = new Vector2(144f, 24f);

        ui.detailRecordButton = CreateButton("DetailRecordButton", detailPanel, "借用历史", 13f, FontWeight.Medium, TextPrimary, TabInactive, new Vector2(108f, 40f));
        RectTransform recordRt = ui.detailRecordButton.GetComponent<RectTransform>();
        recordRt.anchorMin = new Vector2(0f, 0f);
        recordRt.anchorMax = new Vector2(0f, 0f);
        recordRt.pivot = new Vector2(0f, 0f);
        recordRt.anchoredPosition = new Vector2(264f, 24f);
    }

    private static void BuildEquipmentBorrowPanel(GameObject panel, EquipmentPanelUI ui)
    {
        GameObject formCard = CreatePanelCard("BorrowFormCard", panel, PanelBgAlt, PanelBorder);
        SetRect(formCard.GetComponent<RectTransform>(), new Vector2(0f, 0f), new Vector2(0.5f, 1f), new Vector2(0.5f, 0.5f), new Vector2(-12f, 0f), Vector2.zero);

        TextMeshProUGUI title = CreateText("BorrowTitleText", formCard, "借用申请", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform titleRt = title.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(1f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 26f);
        titleRt.anchoredPosition = new Vector2(20f, -18f);

        GameObject formRoot = CreateElement("FormRoot", formCard);
        RectTransform formRt = formRoot.GetComponent<RectTransform>();
        formRt.anchorMin = new Vector2(0f, 0f);
        formRt.anchorMax = new Vector2(1f, 1f);
        formRt.offsetMin = new Vector2(20f, 86f);
        formRt.offsetMax = new Vector2(-20f, -78f);

        VerticalLayoutGroup formLayout = formRoot.AddComponent<VerticalLayoutGroup>();
        formLayout.spacing = 12f;
        formLayout.childControlWidth = true;
        formLayout.childControlHeight = true;
        formLayout.childForceExpandWidth = true;
        formLayout.childForceExpandHeight = false;

        ui.borrowDeviceDropdown = CreateLabeledDropdownField(formRoot, "BorrowDeviceDropdown", "选择设备", new List<string> { "暂无设备" }, 74f);
        ui.borrowApplicantInput = CreateLabeledInputField(formRoot, "BorrowApplicantInput", "申请人姓名", "请输入申请人姓名", 74f, false);
        ui.borrowStartInput = CreateLabeledInputField(formRoot, "BorrowStartInput", "预计开始时间", "如 2026-07-12 09:00", 74f, false);
        ui.borrowEndInput = CreateLabeledInputField(formRoot, "BorrowEndInput", "预计归还时间", "如 2026-07-12 18:00", 74f, false);
        ui.borrowPurposeInput = CreateLabeledInputField(formRoot, "BorrowPurposeInput", "用途说明", "请输入借用用途", 132f, true);

        ui.borrowStatusText = CreateText("BorrowStatusText", formCard, "", 12f, FontWeight.Regular, WarningOrange, TextAlignmentOptions.Left);
        RectTransform statusRt = ui.borrowStatusText.GetComponent<RectTransform>();
        statusRt.anchorMin = new Vector2(0f, 0f);
        statusRt.anchorMax = new Vector2(1f, 0f);
        statusRt.pivot = new Vector2(0f, 0f);
        statusRt.sizeDelta = new Vector2(-40f, 18f);
        statusRt.anchoredPosition = new Vector2(20f, 62f);
        ui.borrowStatusText.gameObject.SetActive(false);

        ui.borrowClearButton = CreateButton("BorrowClearButton", formCard, "清空", 13f, FontWeight.Medium, TextPrimary, TabInactive, new Vector2(104f, 40f));
        RectTransform clearRt = ui.borrowClearButton.GetComponent<RectTransform>();
        clearRt.anchorMin = new Vector2(1f, 0f);
        clearRt.anchorMax = new Vector2(1f, 0f);
        clearRt.pivot = new Vector2(1f, 0f);
        clearRt.anchoredPosition = new Vector2(-136f, 18f);

        ui.borrowSubmitButton = CreateButton("BorrowSubmitButton", formCard, "提交申请", 13f, FontWeight.SemiBold, TextPrimary, AccentBlue, new Vector2(104f, 40f));
        RectTransform submitRt = ui.borrowSubmitButton.GetComponent<RectTransform>();
        submitRt.anchorMin = new Vector2(1f, 0f);
        submitRt.anchorMax = new Vector2(1f, 0f);
        submitRt.pivot = new Vector2(1f, 0f);
        submitRt.anchoredPosition = new Vector2(-20f, 18f);

        ui.borrowSuccessOverlay = CreatePanelCard("BorrowSuccessOverlay", formCard, new Color(0.15f, 0.68f, 0.38f, 0.14f), SuccessGreen);
        RectTransform successRt = ui.borrowSuccessOverlay.GetComponent<RectTransform>();
        successRt.anchorMin = new Vector2(0.5f, 0.5f);
        successRt.anchorMax = new Vector2(0.5f, 0.5f);
        successRt.pivot = new Vector2(0.5f, 0.5f);
        successRt.sizeDelta = new Vector2(280f, 80f);
        successRt.anchoredPosition = Vector2.zero;
        TextMeshProUGUI successText = CreateText("SuccessText", ui.borrowSuccessOverlay, "借用申请已提交", 18f, FontWeight.SemiBold, SuccessGreen, TextAlignmentOptions.Center);
        StretchFull(successText.GetComponent<RectTransform>());
        ui.borrowSuccessOverlay.SetActive(false);

        GameObject guideCard = CreatePanelCard("GuideCard", panel, PanelBgAlt, PanelBorder);
        SetRect(guideCard.GetComponent<RectTransform>(), new Vector2(0.52f, 0f), new Vector2(1f, 1f), new Vector2(1f, 1f), Vector2.zero, Vector2.zero);

        TextMeshProUGUI guideTitle = CreateText("GuideTitleText", guideCard, "流程提示", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform guideTitleRt = guideTitle.GetComponent<RectTransform>();
        guideTitleRt.anchorMin = new Vector2(0f, 1f);
        guideTitleRt.anchorMax = new Vector2(1f, 1f);
        guideTitleRt.pivot = new Vector2(0f, 1f);
        guideTitleRt.sizeDelta = new Vector2(0f, 26f);
        guideTitleRt.anchoredPosition = new Vector2(20f, -18f);

        TextMeshProUGUI guideText = CreateText(
            "GuideText",
            guideCard,
            "1. 选择闲置设备并填写借用时间。\n2. 提交后进入借用记录。\n3. 后续可扩展审批、领用与归还流程。",
            14f,
            FontWeight.Regular,
            TextPrimary,
            TextAlignmentOptions.TopLeft);
        guideText.enableWordWrapping = true;
        RectTransform guideTextRt = guideText.GetComponent<RectTransform>();
        guideTextRt.anchorMin = new Vector2(0f, 1f);
        guideTextRt.anchorMax = new Vector2(1f, 1f);
        guideTextRt.pivot = new Vector2(0f, 1f);
        guideTextRt.sizeDelta = new Vector2(-40f, 160f);
        guideTextRt.anchoredPosition = new Vector2(20f, -62f);

        TextMeshProUGUI noteText = CreateText(
            "NoteText",
            guideCard,
            "当前阶段为 Mock 数据演示，后续可直接替换为后端 API 返回的设备列表与申请接口。",
            12f,
            FontWeight.Regular,
            TextSecondary,
            TextAlignmentOptions.TopLeft);
        noteText.enableWordWrapping = true;
        RectTransform noteRt = noteText.GetComponent<RectTransform>();
        noteRt.anchorMin = new Vector2(0f, 0f);
        noteRt.anchorMax = new Vector2(1f, 0f);
        noteRt.pivot = new Vector2(0f, 0f);
        noteRt.sizeDelta = new Vector2(-40f, 80f);
        noteRt.anchoredPosition = new Vector2(20f, 24f);
    }

    private static void BuildEquipmentRecordPanel(GameObject panel, EquipmentPanelUI ui)
    {
        GameObject card = CreatePanelCard("RecordCard", panel, PanelBgAlt, PanelBorder);
        StretchFull(card.GetComponent<RectTransform>());

        TextMeshProUGUI title = CreateText("RecordTitleText", card, "借用记录", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform titleRt = title.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(1f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 24f);
        titleRt.anchoredPosition = new Vector2(20f, -18f);

        CreateTableHeader(card, new[]
        {
            new HeaderColumn("设备名称", 180f),
            new HeaderColumn("借用人", 120f),
            new HeaderColumn("借出时间", 180f),
            new HeaderColumn("归还时间", 180f),
            new HeaderColumn("状态", 120f)
        }, new Vector2(0f, 1f), new Vector2(1f, 1f), new Vector2(0f, -56f), 42f);

        RectTransform recordContent = CreateVerticalScrollArea(card, "RecordScrollView", new Vector2(0f, 0f), new Vector2(1f, 1f), new Vector2(0f, 20f), new Vector2(0f, -104f), 10f);
        ui.recordListContainer = recordContent;
    }

    private static void BuildEquipmentRepairPanel(GameObject panel, EquipmentPanelUI ui)
    {
        GameObject leftCard = CreatePanelCard("RepairFormCard", panel, PanelBgAlt, PanelBorder);
        SetRect(leftCard.GetComponent<RectTransform>(), new Vector2(0f, 0f), new Vector2(0.45f, 1f), new Vector2(0.5f, 0.5f), new Vector2(-12f, 0f), Vector2.zero);

        TextMeshProUGUI title = CreateText("RepairTitleText", leftCard, "维修上报", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform titleRt = title.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(1f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 24f);
        titleRt.anchoredPosition = new Vector2(20f, -18f);

        GameObject formRoot = CreateElement("FormRoot", leftCard);
        RectTransform formRt = formRoot.GetComponent<RectTransform>();
        formRt.anchorMin = new Vector2(0f, 0f);
        formRt.anchorMax = new Vector2(1f, 1f);
        formRt.offsetMin = new Vector2(20f, 100f);
        formRt.offsetMax = new Vector2(-20f, -74f);

        VerticalLayoutGroup formLayout = formRoot.AddComponent<VerticalLayoutGroup>();
        formLayout.spacing = 12f;
        formLayout.childControlWidth = true;
        formLayout.childControlHeight = true;
        formLayout.childForceExpandWidth = true;
        formLayout.childForceExpandHeight = false;

        ui.repairDeviceDropdown = CreateLabeledDropdownField(formRoot, "RepairDeviceDropdown", "选择设备", new List<string> { "暂无设备" }, 74f);
        ui.repairDescInput = CreateLabeledInputField(formRoot, "RepairDescInput", "故障描述", "请填写故障现象、时间与影响范围", 164f, true);

        ui.repairStatusText = CreateText("RepairStatusText", leftCard, "", 12f, FontWeight.Regular, WarningOrange, TextAlignmentOptions.Left);
        RectTransform statusRt = ui.repairStatusText.GetComponent<RectTransform>();
        statusRt.anchorMin = new Vector2(0f, 0f);
        statusRt.anchorMax = new Vector2(1f, 0f);
        statusRt.pivot = new Vector2(0f, 0f);
        statusRt.sizeDelta = new Vector2(-40f, 18f);
        statusRt.anchoredPosition = new Vector2(20f, 62f);
        ui.repairStatusText.gameObject.SetActive(false);

        ui.repairSubmitButton = CreateButton("RepairSubmitButton", leftCard, "提交报修", 13f, FontWeight.SemiBold, TextPrimary, WarningOrange, new Vector2(112f, 40f));
        RectTransform submitRt = ui.repairSubmitButton.GetComponent<RectTransform>();
        submitRt.anchorMin = new Vector2(1f, 0f);
        submitRt.anchorMax = new Vector2(1f, 0f);
        submitRt.pivot = new Vector2(1f, 0f);
        submitRt.anchoredPosition = new Vector2(-20f, 18f);

        GameObject rightCard = CreatePanelCard("RepairHistoryCard", panel, PanelBgAlt, PanelBorder);
        SetRect(rightCard.GetComponent<RectTransform>(), new Vector2(0.47f, 0f), new Vector2(1f, 1f), new Vector2(1f, 1f), Vector2.zero, Vector2.zero);

        TextMeshProUGUI historyTitle = CreateText("HistoryTitleText", rightCard, "历史维修记录", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform historyTitleRt = historyTitle.GetComponent<RectTransform>();
        historyTitleRt.anchorMin = new Vector2(0f, 1f);
        historyTitleRt.anchorMax = new Vector2(1f, 1f);
        historyTitleRt.pivot = new Vector2(0f, 1f);
        historyTitleRt.sizeDelta = new Vector2(0f, 24f);
        historyTitleRt.anchoredPosition = new Vector2(20f, -18f);

        CreateTableHeader(rightCard, new[]
        {
            new HeaderColumn("设备", 120f),
            new HeaderColumn("上报时间", 150f),
            new HeaderColumn("故障描述", 220f),
            new HeaderColumn("处理人", 100f),
            new HeaderColumn("状态", 100f)
        }, new Vector2(0f, 1f), new Vector2(1f, 1f), new Vector2(0f, -56f), 42f);

        RectTransform historyContent = CreateVerticalScrollArea(rightCard, "RepairHistoryScrollView", new Vector2(0f, 0f), new Vector2(1f, 1f), new Vector2(0f, 20f), new Vector2(0f, -104f), 10f);
        ui.repairRecordListContainer = historyContent;

        ui.repairHistoryPlaceholderText = CreateText("RepairHistoryPlaceholderText", rightCard, "暂无维修记录", 12f, FontWeight.Regular, TextMuted, TextAlignmentOptions.Center);
        RectTransform placeholderRt = ui.repairHistoryPlaceholderText.GetComponent<RectTransform>();
        placeholderRt.anchorMin = new Vector2(0f, 0f);
        placeholderRt.anchorMax = new Vector2(1f, 0f);
        placeholderRt.pivot = new Vector2(0.5f, 0f);
        placeholderRt.sizeDelta = new Vector2(0f, 18f);
        placeholderRt.anchoredPosition = new Vector2(0f, 24f);
    }

    private static void BuildMonitorOverviewPanel(GameObject panel, MonitorPanelUI ui)
    {
        GameObject sensorCard = CreatePanelCard("SensorCard", panel, PanelBgAlt, PanelBorder);
        SetRect(sensorCard.GetComponent<RectTransform>(), new Vector2(0f, 1f), new Vector2(1f, 1f), new Vector2(0.5f, 1f), new Vector2(0f, 250f), new Vector2(0f, 0f));

        TextMeshProUGUI title = CreateText("SensorTitleText", sensorCard, "环境概览", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform titleRt = title.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(0.4f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 24f);
        titleRt.anchoredPosition = new Vector2(20f, -18f);

        GameObject overallPill = CreatePanelCard("OverallStatusPill", sensorCard, new Color(0.15f, 0.68f, 0.38f, 0.14f), Clear);
        SetRect(overallPill.GetComponent<RectTransform>(), new Vector2(1f, 1f), new Vector2(1f, 1f), new Vector2(1f, 1f), new Vector2(148f, 34f), new Vector2(-20f, -16f));
        ui.overallStatusBadge = overallPill.GetComponent<Image>();
        ui.overallStatusText = CreateText("OverallStatusText", overallPill, "正常", 13f, FontWeight.Medium, TextPrimary, TextAlignmentOptions.Center);
        StretchFull(ui.overallStatusText.GetComponent<RectTransform>());

        BuildSensorCard(sensorCard, "TempMetricCard", "温度", "-- °C", new Vector2(20f, -60f), out ui.tempValueText, out ui.tempStatusText, out ui.tempStatusBadge);
        BuildSensorCard(sensorCard, "HumidityMetricCard", "湿度", "-- %", new Vector2(276f, -60f), out ui.humidityValueText, out ui.humidityStatusText, out ui.humidityStatusBadge);
        BuildSensorCard(sensorCard, "SmokeMetricCard", "烟雾", "-- ppm", new Vector2(532f, -60f), out ui.smokeValueText, out ui.smokeStatusText, out ui.smokeStatusBadge);
        BuildSensorCard(sensorCard, "GasMetricCard", "气体浓度", "-- ppm", new Vector2(20f, -150f), out ui.gasValueText, out ui.gasStatusText, out ui.gasStatusBadge);
        BuildSensorCard(sensorCard, "BatteryMetricCard", "设备电量", "-- %", new Vector2(276f, -150f), out ui.batteryValueText, out ui.batteryStatusText, out ui.batteryStatusBadge);
        BuildSensorCard(sensorCard, "SignalMetricCard", "网络信号", "--", new Vector2(532f, -150f), out ui.signalValueText, out ui.signalStatusText, out ui.signalStatusBadge);

        GameObject deviceCard = CreatePanelCard("DeviceSummaryCard", panel, PanelBgAlt, PanelBorder);
        RectTransform deviceCardRt = deviceCard.GetComponent<RectTransform>();
        deviceCardRt.anchorMin = new Vector2(0f, 0f);
        deviceCardRt.anchorMax = new Vector2(0.56f, 1f);
        deviceCardRt.pivot = new Vector2(0f, 0f);
        deviceCardRt.offsetMin = Vector2.zero;
        deviceCardRt.offsetMax = new Vector2(0f, -270f);

        TextMeshProUGUI deviceTitle = CreateText("DeviceSummaryTitleText", deviceCard, "设备实时状态", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform deviceTitleRt = deviceTitle.GetComponent<RectTransform>();
        deviceTitleRt.anchorMin = new Vector2(0f, 1f);
        deviceTitleRt.anchorMax = new Vector2(1f, 1f);
        deviceTitleRt.pivot = new Vector2(0f, 1f);
        deviceTitleRt.sizeDelta = new Vector2(0f, 24f);
        deviceTitleRt.anchoredPosition = new Vector2(20f, -18f);

        CreateTableHeader(deviceCard, new[]
        {
            new HeaderColumn("设备", 150f),
            new HeaderColumn("状态", 100f),
            new HeaderColumn("健康分", 80f),
            new HeaderColumn("温度", 80f),
            new HeaderColumn("湿度", 80f),
            new HeaderColumn("电量", 80f)
        }, new Vector2(0f, 1f), new Vector2(1f, 1f), new Vector2(0f, -56f), 42f);

        RectTransform deviceContent = CreateVerticalScrollArea(deviceCard, "DeviceSummaryScrollView", new Vector2(0f, 0f), new Vector2(1f, 1f), new Vector2(0f, 20f), new Vector2(0f, -104f), 10f);
        ui.deviceTableContainer = deviceContent;

        GameObject alertCard = CreatePanelCard("OverviewAlertCard", panel, PanelBgAlt, PanelBorder);
        RectTransform alertCardRt = alertCard.GetComponent<RectTransform>();
        alertCardRt.anchorMin = new Vector2(0.58f, 0f);
        alertCardRt.anchorMax = new Vector2(1f, 1f);
        alertCardRt.pivot = new Vector2(0f, 0f);
        alertCardRt.offsetMin = Vector2.zero;
        alertCardRt.offsetMax = new Vector2(0f, -270f);

        TextMeshProUGUI alertTitle = CreateText("OverviewAlertTitleText", alertCard, "最近告警记录", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform alertTitleRt = alertTitle.GetComponent<RectTransform>();
        alertTitleRt.anchorMin = new Vector2(0f, 1f);
        alertTitleRt.anchorMax = new Vector2(1f, 1f);
        alertTitleRt.pivot = new Vector2(0f, 1f);
        alertTitleRt.sizeDelta = new Vector2(0f, 24f);
        alertTitleRt.anchoredPosition = new Vector2(20f, -18f);

        RectTransform alertContent = CreateVerticalScrollArea(alertCard, "OverviewAlertScrollView", new Vector2(0f, 0f), new Vector2(1f, 1f), new Vector2(0f, 20f), new Vector2(0f, -56f), 10f);
        ui.overviewAlertListContainer = alertContent;
    }

    private static void BuildMonitorDevicePanel(GameObject panel, MonitorPanelUI ui)
    {
        GameObject card = CreatePanelCard("DeviceDetailCard", panel, PanelBgAlt, PanelBorder);
        StretchFull(card.GetComponent<RectTransform>());

        TextMeshProUGUI title = CreateText("DeviceDetailTitleText", card, "设备状态详情", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform titleRt = title.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(1f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 24f);
        titleRt.anchoredPosition = new Vector2(20f, -18f);

        CreateTableHeader(card, new[]
        {
            new HeaderColumn("设备", 180f),
            new HeaderColumn("状态", 110f),
            new HeaderColumn("健康分", 90f),
            new HeaderColumn("温度", 90f),
            new HeaderColumn("湿度", 90f),
            new HeaderColumn("电量", 90f)
        }, new Vector2(0f, 1f), new Vector2(1f, 1f), new Vector2(0f, -56f), 42f);

        RectTransform content = CreateVerticalScrollArea(card, "DeviceDetailScrollView", new Vector2(0f, 0f), new Vector2(1f, 1f), new Vector2(0f, 20f), new Vector2(0f, -104f), 10f);
        ui.deviceDetailContainer = content;
    }

    private static void BuildMonitorAlertPanel(GameObject panel, MonitorPanelUI ui)
    {
        GameObject card = CreatePanelCard("AlertCard", panel, PanelBgAlt, PanelBorder);
        StretchFull(card.GetComponent<RectTransform>());

        TextMeshProUGUI title = CreateText("AlertTitleText", card, "告警记录", 18f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform titleRt = title.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(1f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 24f);
        titleRt.anchoredPosition = new Vector2(20f, -18f);

        RectTransform content = CreateVerticalScrollArea(card, "AlertScrollView", new Vector2(0f, 0f), new Vector2(1f, 1f), new Vector2(0f, 20f), new Vector2(0f, -56f), 10f);
        ui.alertListContainer = content;
    }

    private static GameObject BuildReagentCardTemplate(GameObject parent)
    {
        GameObject card = CreatePanelCard("ReagentCardTemplate", parent, CardBg, PanelBorder);
        card.SetActive(false);
        RectTransform rt = card.GetComponent<RectTransform>();
        rt.sizeDelta = new Vector2(246f, 124f);

        Image accent = CreateElement("AccentBar", card).AddComponent<Image>();
        ApplyDefaultSprite(accent);
        accent.color = AccentBlue;
        RectTransform accentRt = accent.GetComponent<RectTransform>();
        accentRt.anchorMin = new Vector2(0f, 0f);
        accentRt.anchorMax = new Vector2(0f, 1f);
        accentRt.pivot = new Vector2(0f, 0.5f);
        accentRt.sizeDelta = new Vector2(4f, 0f);
        accentRt.anchoredPosition = Vector2.zero;

        Button button = card.AddComponent<Button>();
        button.targetGraphic = card.GetComponent<Image>();

        TextMeshProUGUI nameText = CreateText("NameText", card, "氯化钠", 17f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform nameRt = nameText.GetComponent<RectTransform>();
        nameRt.anchorMin = new Vector2(0f, 1f);
        nameRt.anchorMax = new Vector2(1f, 1f);
        nameRt.pivot = new Vector2(0f, 1f);
        nameRt.sizeDelta = new Vector2(-36f, 24f);
        nameRt.anchoredPosition = new Vector2(18f, -16f);

        TextMeshProUGUI categoryText = CreateText("CategoryText", card, "无机盐", 12f, FontWeight.Regular, TextSecondary, TextAlignmentOptions.Left);
        RectTransform categoryRt = categoryText.GetComponent<RectTransform>();
        categoryRt.anchorMin = new Vector2(0f, 1f);
        categoryRt.anchorMax = new Vector2(1f, 1f);
        categoryRt.pivot = new Vector2(0f, 1f);
        categoryRt.sizeDelta = new Vector2(-36f, 18f);
        categoryRt.anchoredPosition = new Vector2(18f, -42f);

        TextMeshProUGUI stockText = CreateText("StockText", card, "库存：50g", 13f, FontWeight.Medium, TextPrimary, TextAlignmentOptions.Left);
        RectTransform stockRt = stockText.GetComponent<RectTransform>();
        stockRt.anchorMin = new Vector2(0f, 1f);
        stockRt.anchorMax = new Vector2(1f, 1f);
        stockRt.pivot = new Vector2(0f, 1f);
        stockRt.sizeDelta = new Vector2(-36f, 18f);
        stockRt.anchoredPosition = new Vector2(18f, -66f);

        TextMeshProUGUI batchText = CreateText("BatchText", card, "批号：B2024-001", 12f, FontWeight.Regular, TextSecondary, TextAlignmentOptions.Left);
        RectTransform batchRt = batchText.GetComponent<RectTransform>();
        batchRt.anchorMin = new Vector2(0f, 1f);
        batchRt.anchorMax = new Vector2(1f, 1f);
        batchRt.pivot = new Vector2(0f, 1f);
        batchRt.sizeDelta = new Vector2(-36f, 18f);
        batchRt.anchoredPosition = new Vector2(18f, -88f);

        Image statusBadge = CreatePanelCard("StatusBadge", card, new Color(0.15f, 0.68f, 0.38f, 0.16f), Clear).GetComponent<Image>();
        RectTransform badgeRt = statusBadge.GetComponent<RectTransform>();
        badgeRt.anchorMin = new Vector2(1f, 0f);
        badgeRt.anchorMax = new Vector2(1f, 0f);
        badgeRt.pivot = new Vector2(1f, 0f);
        badgeRt.sizeDelta = new Vector2(76f, 28f);
        badgeRt.anchoredPosition = new Vector2(-14f, 12f);

        TextMeshProUGUI statusText = CreateText("StatusText", statusBadge.gameObject, "正常", 12f, FontWeight.Medium, TextPrimary, TextAlignmentOptions.Center);
        StretchFull(statusText.GetComponent<RectTransform>());

        TextMeshProUGUI actionText = CreateText("ActionHintText", card, "点击查看详情", 11f, FontWeight.Regular, AccentBlue, TextAlignmentOptions.Right);
        RectTransform actionRt = actionText.GetComponent<RectTransform>();
        actionRt.anchorMin = new Vector2(0f, 0f);
        actionRt.anchorMax = new Vector2(1f, 0f);
        actionRt.pivot = new Vector2(1f, 0f);
        actionRt.sizeDelta = new Vector2(-30f, 16f);
        actionRt.anchoredPosition = new Vector2(-96f, 18f);

        return card;
    }

    private static GameObject BuildReagentBatchRowTemplate(GameObject parent)
    {
        GameObject row = CreateRowTemplate("ReagentBatchRowTemplate", parent, 40f);
        row.SetActive(false);

        CreateCellText(row, "BatchNoText", "B2024-001", 110f);
        CreateCellText(row, "EntryDateText", "2025-03-01", 110f);
        CreateCellText(row, "RemainText", "50/100g", 100f);
        CreateStatusCell(row, "StatusBadge", "StatusText", 90f);

        return row;
    }

    private static GameObject BuildPickupRecordRowTemplate(GameObject parent)
    {
        GameObject row = CreateRowTemplate("PickupRecordRowTemplate", parent, 44f);
        row.SetActive(false);

        CreateCellText(row, "ApplicantText", "王同学", 120f);
        CreateCellText(row, "ReagentText", "氯化钠", 180f);
        CreateCellText(row, "QuantityText", "20g", 100f);
        CreateCellText(row, "PickupTimeText", "2026-07-09 10:15", 180f);
        CreateStatusCell(row, "StatusBadge", "StatusText", 120f);

        return row;
    }

    private static GameObject BuildEquipmentCardTemplate(GameObject parent)
    {
        GameObject card = CreatePanelCard("EquipmentCardTemplate", parent, CardBg, PanelBorder);
        card.SetActive(false);
        RectTransform rt = card.GetComponent<RectTransform>();
        rt.sizeDelta = new Vector2(246f, 132f);

        Image accent = CreateElement("AccentBar", card).AddComponent<Image>();
        ApplyDefaultSprite(accent);
        accent.color = AccentBlue;
        RectTransform accentRt = accent.GetComponent<RectTransform>();
        accentRt.anchorMin = new Vector2(0f, 0f);
        accentRt.anchorMax = new Vector2(0f, 1f);
        accentRt.pivot = new Vector2(0f, 0.5f);
        accentRt.sizeDelta = new Vector2(4f, 0f);
        accentRt.anchoredPosition = Vector2.zero;

        Button button = card.AddComponent<Button>();
        button.targetGraphic = card.GetComponent<Image>();

        TextMeshProUGUI nameText = CreateText("NameText", card, "生物显微镜", 17f, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform nameRt = nameText.GetComponent<RectTransform>();
        nameRt.anchorMin = new Vector2(0f, 1f);
        nameRt.anchorMax = new Vector2(1f, 1f);
        nameRt.pivot = new Vector2(0f, 1f);
        nameRt.sizeDelta = new Vector2(-36f, 24f);
        nameRt.anchoredPosition = new Vector2(18f, -16f);

        TextMeshProUGUI noText = CreateText("NoText", card, "EQ-2025-0042", 12f, FontWeight.Regular, TextSecondary, TextAlignmentOptions.Left);
        RectTransform noRt = noText.GetComponent<RectTransform>();
        noRt.anchorMin = new Vector2(0f, 1f);
        noRt.anchorMax = new Vector2(1f, 1f);
        noRt.pivot = new Vector2(0f, 1f);
        noRt.sizeDelta = new Vector2(-36f, 18f);
        noRt.anchoredPosition = new Vector2(18f, -42f);

        Image statusBadge = CreatePanelCard("StatusBadge", card, new Color(0.15f, 0.68f, 0.38f, 0.16f), Clear).GetComponent<Image>();
        RectTransform badgeRt = statusBadge.GetComponent<RectTransform>();
        badgeRt.anchorMin = new Vector2(0f, 1f);
        badgeRt.anchorMax = new Vector2(0f, 1f);
        badgeRt.pivot = new Vector2(0f, 1f);
        badgeRt.sizeDelta = new Vector2(92f, 28f);
        badgeRt.anchoredPosition = new Vector2(18f, -70f);

        TextMeshProUGUI statusText = CreateText("StatusText", statusBadge.gameObject, "闲置", 12f, FontWeight.Medium, TextPrimary, TextAlignmentOptions.Center);
        StretchFull(statusText.GetComponent<RectTransform>());

        TextMeshProUGUI healthText = CreateText("HealthText", card, "健康分: 92", 12f, FontWeight.Medium, TextPrimary, TextAlignmentOptions.Left);
        RectTransform healthTextRt = healthText.GetComponent<RectTransform>();
        healthTextRt.anchorMin = new Vector2(0f, 1f);
        healthTextRt.anchorMax = new Vector2(1f, 1f);
        healthTextRt.pivot = new Vector2(0f, 1f);
        healthTextRt.sizeDelta = new Vector2(-36f, 18f);
        healthTextRt.anchoredPosition = new Vector2(18f, -100f);

        Slider healthSlider = CreateSlider(card, "HealthSlider", new Vector2(100f, 12f), SuccessGreen);
        RectTransform sliderRt = healthSlider.GetComponent<RectTransform>();
        sliderRt.anchorMin = new Vector2(1f, 1f);
        sliderRt.anchorMax = new Vector2(1f, 1f);
        sliderRt.pivot = new Vector2(1f, 1f);
        sliderRt.anchoredPosition = new Vector2(-18f, -102f);

        return card;
    }

    private static GameObject BuildBorrowRecordRowTemplate(GameObject parent)
    {
        GameObject row = CreateRowTemplate("BorrowRecordRowTemplate", parent, 44f);
        row.SetActive(false);

        CreateCellText(row, "DeviceNameText", "电子分析天平", 180f);
        CreateCellText(row, "BorrowerText", "张同学", 120f);
        CreateCellText(row, "OutTimeText", "2026-07-08 09:00", 180f);
        CreateCellText(row, "ReturnTimeText", "2026-07-08 18:00", 180f);
        CreateStatusCell(row, "StatusBadge", "StatusText", 120f);

        return row;
    }

    private static GameObject BuildRepairRecordRowTemplate(GameObject parent)
    {
        GameObject row = CreateRowTemplate("RepairRecordRowTemplate", parent, 52f);
        row.SetActive(false);

        CreateCellText(row, "DeviceNameText", "台式离心机", 120f);
        CreateCellText(row, "ReportTimeText", "2026-07-10 14:32", 150f);
        TextMeshProUGUI descText = CreateCellText(row, "DescText", "高速运转时温度异常升高", -1f);
        descText.enableWordWrapping = true;
        CreateCellText(row, "HandlerText", "设备管理员", 100f);
        CreateStatusCell(row, "StatusBadge", "StatusText", 100f);

        return row;
    }

    private static GameObject BuildMonitorDeviceRowTemplate(GameObject parent)
    {
        GameObject row = CreateRowTemplate("MonitorDeviceRowTemplate", parent, 44f);
        row.SetActive(false);

        CreateCellText(row, "DeviceNameText", "生物显微镜", 180f);
        CreateStatusCell(row, "StatusBadge", "StatusText", 110f);
        CreateCellText(row, "HealthText", "92", 90f, TextAlignmentOptions.Center);
        CreateCellText(row, "TempText", "22°C", 90f, TextAlignmentOptions.Center);
        CreateCellText(row, "HumidityText", "45%", 90f, TextAlignmentOptions.Center);
        CreateCellText(row, "BatteryText", "85%", 90f, TextAlignmentOptions.Center);

        return row;
    }

    private static GameObject BuildAlertRowTemplate(GameObject parent)
    {
        GameObject row = CreateRowTemplate("AlertRowTemplate", parent, 76f);
        row.SetActive(false);

        GameObject levelWrap = CreateElement("LevelWrap", row);
        HorizontalLayoutGroup levelLayout = levelWrap.AddComponent<HorizontalLayoutGroup>();
        levelLayout.spacing = 8f;
        levelLayout.childAlignment = TextAnchor.MiddleLeft;
        levelLayout.childControlHeight = true;
        levelLayout.childControlWidth = true;
        levelLayout.childForceExpandHeight = false;
        levelLayout.childForceExpandWidth = false;
        LayoutElement levelElement = levelWrap.AddComponent<LayoutElement>();
        levelElement.preferredWidth = 92f;
        levelElement.minWidth = 80f;

        Image levelBadge = CreatePanelCard("LevelBadge", levelWrap, new Color(0.91f, 0.3f, 0.24f, 0.14f), Clear).GetComponent<Image>();
        RectTransform badgeRt = levelBadge.GetComponent<RectTransform>();
        badgeRt.sizeDelta = new Vector2(28f, 28f);

        TextMeshProUGUI iconText = CreateText("LevelIconText", levelBadge.gameObject, "●", 13f, FontWeight.Bold, TextPrimary, TextAlignmentOptions.Center);
        StretchFull(iconText.GetComponent<RectTransform>());

        CreateCellText(levelWrap, "LevelText", "告警", -1f);

        CreateCellText(row, "TimeText", "14:32", 66f, TextAlignmentOptions.Center);

        GameObject detailWrap = CreateElement("DetailWrap", row);
        VerticalLayoutGroup detailLayout = detailWrap.AddComponent<VerticalLayoutGroup>();
        detailLayout.spacing = 2f;
        detailLayout.childAlignment = TextAnchor.UpperLeft;
        detailLayout.childControlWidth = true;
        detailLayout.childControlHeight = true;
        detailLayout.childForceExpandWidth = true;
        detailLayout.childForceExpandHeight = false;
        LayoutElement detailElement = detailWrap.AddComponent<LayoutElement>();
        detailElement.flexibleWidth = 1f;

        TextMeshProUGUI deviceText = CreateCellText(detailWrap, "DeviceText", "台式离心机", -1f);
        deviceText.fontWeight = FontWeight.SemiBold;
        deviceText.fontSize = 12f;

        TextMeshProUGUI messageText = CreateCellText(detailWrap, "MessageText", "温度过高 (52°C)，已超过安全阈值 40°C", -1f);
        messageText.enableWordWrapping = true;
        messageText.alignment = TextAlignmentOptions.TopLeft;
        messageText.fontSize = 11.5f;
        messageText.color = TextSecondary;

        return row;
    }

    private static GameObject BuildPromptHUD(out TextMeshProUGUI promptText)
    {
        DestroyIfExists("InteractionHUD");

        GameObject canvasGo = new GameObject("InteractionHUD", typeof(RectTransform), typeof(Canvas), typeof(CanvasScaler), typeof(GraphicRaycaster));
        Canvas canvas = canvasGo.GetComponent<Canvas>();
        canvas.renderMode = RenderMode.ScreenSpaceOverlay;
        canvas.sortingOrder = 200;

        CanvasScaler scaler = canvasGo.GetComponent<CanvasScaler>();
        scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
        scaler.referenceResolution = new Vector2(1920f, 1080f);
        scaler.matchWidthOrHeight = 0.5f;

        promptText = CreateText("PromptText", canvasGo, "按 Space 查看交互面板", 22f, FontWeight.Medium, new Color(0.95f, 0.97f, 1f, 0.9f), TextAlignmentOptions.Center);
        promptText.outlineWidth = 0.25f;
        promptText.outlineColor = new Color(0f, 0f, 0f, 0.6f);
        RectTransform textRt = promptText.GetComponent<RectTransform>();
        textRt.anchorMin = new Vector2(0.5f, 0f);
        textRt.anchorMax = new Vector2(0.5f, 0f);
        textRt.pivot = new Vector2(0.5f, 0.5f);
        textRt.sizeDelta = new Vector2(720f, 40f);
        textRt.anchoredPosition = new Vector2(0f, 100f);

        return canvasGo;
    }

    private static GameObject BuildInteractionTriggers(GameObject reagentPanel, GameObject equipmentPanel, GameObject monitorPanel, GameObject promptUI, TextMeshProUGUI promptText)
    {
        DestroyIfExists("ReagentAndEquipmentInteraction");

        GameObject root = new GameObject("ReagentAndEquipmentInteraction");
        Transform parent = FindSceneObjectByName("InteractiveScenes")?.transform;
        if (parent != null)
            root.transform.SetParent(parent, false);

        Transform player = FindSceneObjectByName("player")?.transform;
        Vector3 anchor = parent != null ? parent.position : (player != null ? player.position + player.forward * 4f : Vector3.zero);

        CreateTrigger(root.transform, "ReagentTrigger", anchor + new Vector3(3.0f, -0.8f, 3.2f), new Vector3(3.4f, 2.4f, 3.0f), "按 <color=#FFD700>Space</color> 查看药材与试剂", reagentPanel, promptUI, promptText);
        CreateTrigger(root.transform, "EquipmentTrigger", anchor + new Vector3(-0.8f, -0.8f, -1.8f), new Vector3(3.4f, 2.4f, 3.0f), "按 <color=#FFD700>Space</color> 查看实验器材", equipmentPanel, promptUI, promptText);
        CreateTrigger(root.transform, "MonitorTrigger", anchor + new Vector3(5.3f, -0.6f, 0.1f), new Vector3(3.2f, 2.4f, 2.8f), "按 <color=#FFD700>Space</color> 查看设备监控", monitorPanel, promptUI, promptText);

        return root;
    }

    private static void CreateTrigger(Transform parent, string name, Vector3 worldPosition, Vector3 size, string promptMessage, GameObject panel, GameObject promptUI, TextMeshProUGUI promptText)
    {
        GameObject triggerGo = new GameObject(name);
        triggerGo.transform.SetParent(parent, false);
        triggerGo.transform.position = worldPosition;
        triggerGo.transform.localScale = Vector3.one;

        BoxCollider collider = triggerGo.AddComponent<BoxCollider>();
        collider.isTrigger = true;
        collider.size = size;

        ScreenTrigger trigger = triggerGo.AddComponent<ScreenTrigger>();
        trigger.promptUI = promptUI;
        trigger.promptText = promptText;
        trigger.labInfoPanel = panel;
        trigger.promptMessage = promptMessage;
        trigger.playerTag = "Player";

        EditorUtility.SetDirty(triggerGo);
        EditorUtility.SetDirty(trigger);
    }

    private static void EnsureReagentDataProvider()
    {
        if (FindSceneDataProvider() != null)
            return;

        GameObject go = new GameObject("ReagentDataProvider");
        go.AddComponent<ReagentDataProvider>();
    }

    private static void EnsureEventSystem()
    {
        if (Object.FindObjectOfType<EventSystem>() != null)
            return;

        GameObject eventSystem = new GameObject("EventSystem");
        eventSystem.AddComponent<EventSystem>();
        eventSystem.AddComponent<StandaloneInputModule>();
    }

    private static void EnsureIMEHandler()
    {
        // 确保 WebGL 运行时存在 IME Handler（编辑器验证 + 运行时兜底）
        if (Object.FindObjectOfType<WebGLIMEHandler>() != null)
            return;

        GameObject imeGo = new GameObject("IMEHandler");
        imeGo.AddComponent<WebGLIMEHandler>();
        Debug.Log("[Builder] 已自动创建 WebGLIMEHandler");
    }

    private static ReagentDataProvider FindSceneDataProvider()
    {
        ReagentDataProvider[] providers = Resources.FindObjectsOfTypeAll<ReagentDataProvider>();
        foreach (ReagentDataProvider provider in providers)
        {
            if (provider != null && provider.gameObject.scene.IsValid())
                return provider;
        }

        return null;
    }

    private static PanelShell CreatePanelShell<T>(string rootName, string title, string subTitle, int sortingOrder, Vector2 panelSize, out T controller) where T : MonoBehaviour
    {
        DestroyIfExists(rootName);

        GameObject root = CreateElement(rootName, null);
        Canvas canvas = root.AddComponent<Canvas>();
        canvas.renderMode = RenderMode.ScreenSpaceOverlay;
        canvas.sortingOrder = sortingOrder;

        CanvasScaler scaler = root.AddComponent<CanvasScaler>();
        scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
        scaler.referenceResolution = new Vector2(1920f, 1080f);
        scaler.matchWidthOrHeight = 0.5f;

        root.AddComponent<GraphicRaycaster>();
        controller = root.AddComponent<T>();

        GameObject overlay = CreateElement("OverlayBg", root);
        StretchFull(overlay.GetComponent<RectTransform>());
        overlay.AddComponent<Image>().color = OverlayBg;

        GameObject mainPanel = CreatePanelCard("MainPanel", overlay, PanelBg, PanelBorder);
        RectTransform mainRt = mainPanel.GetComponent<RectTransform>();
        mainRt.anchorMin = new Vector2(0.5f, 0.5f);
        mainRt.anchorMax = new Vector2(0.5f, 0.5f);
        mainRt.pivot = new Vector2(0.5f, 0.5f);
        mainRt.sizeDelta = panelSize;
        mainRt.anchoredPosition = Vector2.zero;

        TextMeshProUGUI titleText = CreateText("TitleText", mainPanel, title, 26f, FontWeight.Bold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform titleRt = titleText.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(0.75f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 30f);
        titleRt.anchoredPosition = new Vector2(28f, -18f);

        TextMeshProUGUI subtitleText = CreateText("SubtitleText", mainPanel, subTitle, 12f, FontWeight.Regular, TextSecondary, TextAlignmentOptions.Left);
        RectTransform subtitleRt = subtitleText.GetComponent<RectTransform>();
        subtitleRt.anchorMin = new Vector2(0f, 1f);
        subtitleRt.anchorMax = new Vector2(0.85f, 1f);
        subtitleRt.pivot = new Vector2(0f, 1f);
        subtitleRt.sizeDelta = new Vector2(0f, 18f);
        subtitleRt.anchoredPosition = new Vector2(30f, -48f);

        Button closeButton = CreateButton("CloseButton", mainPanel, "X", 18f, FontWeight.Bold, TextSecondary, TabInactive, new Vector2(38f, 38f));
        RectTransform closeRt = closeButton.GetComponent<RectTransform>();
        closeRt.anchorMin = new Vector2(1f, 1f);
        closeRt.anchorMax = new Vector2(1f, 1f);
        closeRt.pivot = new Vector2(1f, 1f);
        closeRt.anchoredPosition = new Vector2(-20f, -18f);

        CreateSeparator(mainPanel, -76f);

        GameObject tabBar = CreateElement("TabBar", mainPanel);
        RectTransform tabBarRt = tabBar.GetComponent<RectTransform>();
        tabBarRt.anchorMin = new Vector2(0f, 1f);
        tabBarRt.anchorMax = new Vector2(1f, 1f);
        tabBarRt.pivot = new Vector2(0.5f, 1f);
        tabBarRt.sizeDelta = new Vector2(0f, 48f);
        tabBarRt.anchoredPosition = new Vector2(0f, -88f);

        CreateSeparator(mainPanel, -136f);

        GameObject bodyRoot = CreateElement("BodyRoot", mainPanel);
        RectTransform bodyRt = bodyRoot.GetComponent<RectTransform>();
        bodyRt.anchorMin = new Vector2(0f, 0f);
        bodyRt.anchorMax = new Vector2(1f, 1f);
        bodyRt.offsetMin = new Vector2(24f, 24f);
        bodyRt.offsetMax = new Vector2(-24f, -150f);

        return new PanelShell
        {
            root = root,
            overlay = overlay,
            mainPanel = mainPanel,
            tabBar = tabBar,
            bodyRoot = bodyRoot,
            titleText = titleText,
            closeButton = closeButton
        };
    }

    private static void CreateTabButtons(GameObject tabBar, string[] labels, out Button[] buttons, out GameObject[] highlights)
    {
        buttons = new Button[labels.Length];
        highlights = new GameObject[labels.Length];

        float startX = 28f;
        float width = labels.Length >= 4 ? 156f : 188f;
        float spacing = 12f;

        for (int i = 0; i < labels.Length; i++)
        {
            Button button = CreateButton($"Tab{i}", tabBar, labels[i], 14f, FontWeight.SemiBold, i == 0 ? TextPrimary : TextSecondary, i == 0 ? TabActive : TabInactive, new Vector2(width, 38f));
            button.name = i switch
            {
                0 => labels.Length == 4 ? "TabList" : "TabOverview",
                1 => labels.Length == 4 ? "TabBorrow" : labels.Length == 3 ? "TabPickup" : "TabBorrow",
                2 => labels.Length == 4 ? "TabRecord" : labels.Length == 3 ? "TabWarning" : "TabAlert",
                _ => "TabRepair"
            };

            RectTransform rt = button.GetComponent<RectTransform>();
            rt.anchorMin = new Vector2(0f, 0.5f);
            rt.anchorMax = new Vector2(0f, 0.5f);
            rt.pivot = new Vector2(0f, 0.5f);
            rt.anchoredPosition = new Vector2(startX + i * (width + spacing), 0f);

            GameObject highlight = CreateElement(button.name + "Highlight", button.gameObject);
            RectTransform highlightRt = highlight.GetComponent<RectTransform>();
            highlightRt.anchorMin = new Vector2(0f, 0f);
            highlightRt.anchorMax = new Vector2(1f, 0f);
            highlightRt.pivot = new Vector2(0.5f, 0f);
            highlightRt.sizeDelta = new Vector2(0f, 3f);
            highlightRt.anchoredPosition = Vector2.zero;
            Image highlightImage = highlight.AddComponent<Image>();
            ApplyDefaultSprite(highlightImage);
            highlightImage.color = AccentBlue;
            highlight.SetActive(i == 0);

            buttons[i] = button;
            highlights[i] = highlight;
        }
    }

    private static GameObject CreateModalPanel(GameObject parent, string name, Vector2 size)
    {
        GameObject modal = CreatePanelCard(name, parent, PanelBg, PanelBorder);
        RectTransform rt = modal.GetComponent<RectTransform>();
        rt.anchorMin = new Vector2(0.5f, 0.5f);
        rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.sizeDelta = size;
        rt.anchoredPosition = Vector2.zero;
        return modal;
    }

    private static void BuildSensorCard(GameObject parent, string name, string label, string value, Vector2 anchoredPosition, out TextMeshProUGUI valueText, out TextMeshProUGUI statusText, out Image statusBadge)
    {
        GameObject card = CreatePanelCard(name, parent, CardBg, PanelBorder);
        RectTransform rt = card.GetComponent<RectTransform>();
        rt.anchorMin = new Vector2(0f, 1f);
        rt.anchorMax = new Vector2(0f, 1f);
        rt.pivot = new Vector2(0f, 1f);
        rt.sizeDelta = new Vector2(236f, 74f);
        rt.anchoredPosition = anchoredPosition;

        TextMeshProUGUI labelText = CreateText("LabelText", card, label, 13f, FontWeight.Medium, TextSecondary, TextAlignmentOptions.Left);
        RectTransform labelRt = labelText.GetComponent<RectTransform>();
        labelRt.anchorMin = new Vector2(0f, 1f);
        labelRt.anchorMax = new Vector2(1f, 1f);
        labelRt.pivot = new Vector2(0f, 1f);
        labelRt.sizeDelta = new Vector2(0f, 18f);
        labelRt.anchoredPosition = new Vector2(16f, -12f);

        valueText = CreateText(name.Replace("MetricCard", "ValueText"), card, value, 22f, FontWeight.Bold, TextPrimary, TextAlignmentOptions.Left);
        RectTransform valueRt = valueText.GetComponent<RectTransform>();
        valueRt.anchorMin = new Vector2(0f, 1f);
        valueRt.anchorMax = new Vector2(0.8f, 1f);
        valueRt.pivot = new Vector2(0f, 1f);
        valueRt.sizeDelta = new Vector2(0f, 28f);
        valueRt.anchoredPosition = new Vector2(16f, -34f);

        GameObject badge = CreatePanelCard("StatusBadge", card, new Color(0.15f, 0.68f, 0.38f, 0.14f), Clear);
        RectTransform badgeRt = badge.GetComponent<RectTransform>();
        badgeRt.anchorMin = new Vector2(1f, 0f);
        badgeRt.anchorMax = new Vector2(1f, 0f);
        badgeRt.pivot = new Vector2(1f, 0f);
        badgeRt.sizeDelta = new Vector2(74f, 26f);
        badgeRt.anchoredPosition = new Vector2(-12f, 10f);
        statusBadge = badge.GetComponent<Image>();

        statusText = CreateText(name.Replace("MetricCard", "StatusText"), badge, "正常", 12f, FontWeight.Medium, TextPrimary, TextAlignmentOptions.Center);
        StretchFull(statusText.GetComponent<RectTransform>());
    }

    private static RectTransform CreateGridScrollArea(GameObject parent, string name, Vector2 anchorMin, Vector2 anchorMax, Vector2 offsetMin, Vector2 offsetMax, Vector2 cellSize, Vector2 spacing, int columnCount)
    {
        GameObject scrollView = CreateElement(name, parent);
        RectTransform scrollRt = scrollView.GetComponent<RectTransform>();
        scrollRt.anchorMin = anchorMin;
        scrollRt.anchorMax = anchorMax;
        scrollRt.offsetMin = offsetMin;
        scrollRt.offsetMax = offsetMax;

        Image bg = scrollView.AddComponent<Image>();
        ApplyDefaultSprite(bg);
        bg.color = Clear;

        ScrollRect scrollRect = scrollView.AddComponent<ScrollRect>();
        scrollRect.horizontal = false;
        scrollRect.vertical = true;
        scrollRect.movementType = ScrollRect.MovementType.Clamped;
        scrollRect.scrollSensitivity = 28f;

        GameObject viewport = CreateElement("Viewport", scrollView);
        StretchFull(viewport.GetComponent<RectTransform>());
        viewport.AddComponent<RectMask2D>();

        GameObject content = CreateElement("Content", viewport);
        RectTransform contentRt = content.GetComponent<RectTransform>();
        contentRt.anchorMin = new Vector2(0f, 1f);
        contentRt.anchorMax = new Vector2(1f, 1f);
        contentRt.pivot = new Vector2(0.5f, 1f);
        contentRt.anchoredPosition = Vector2.zero;
        contentRt.sizeDelta = new Vector2(0f, 0f);

        GridLayoutGroup grid = content.AddComponent<GridLayoutGroup>();
        grid.cellSize = cellSize;
        grid.spacing = spacing;
        grid.padding = new RectOffset(0, 0, 0, 0);
        grid.childAlignment = TextAnchor.UpperLeft;
        grid.constraint = GridLayoutGroup.Constraint.FixedColumnCount;
        grid.constraintCount = columnCount;

        ContentSizeFitter fitter = content.AddComponent<ContentSizeFitter>();
        fitter.verticalFit = ContentSizeFitter.FitMode.PreferredSize;

        scrollRect.viewport = viewport.GetComponent<RectTransform>();
        scrollRect.content = contentRt;

        return contentRt;
    }

    private static RectTransform CreateVerticalScrollArea(GameObject parent, string name, Vector2 anchorMin, Vector2 anchorMax, Vector2 offsetMin, Vector2 offsetMax, float spacing)
    {
        GameObject scrollView = CreateElement(name, parent);
        RectTransform scrollRt = scrollView.GetComponent<RectTransform>();
        scrollRt.anchorMin = anchorMin;
        scrollRt.anchorMax = anchorMax;
        scrollRt.offsetMin = offsetMin;
        scrollRt.offsetMax = offsetMax;

        Image bg = scrollView.AddComponent<Image>();
        ApplyDefaultSprite(bg);
        bg.color = Clear;

        ScrollRect scrollRect = scrollView.AddComponent<ScrollRect>();
        scrollRect.horizontal = false;
        scrollRect.vertical = true;
        scrollRect.movementType = ScrollRect.MovementType.Clamped;
        scrollRect.scrollSensitivity = 28f;

        GameObject viewport = CreateElement("Viewport", scrollView);
        StretchFull(viewport.GetComponent<RectTransform>());
        viewport.AddComponent<RectMask2D>();

        GameObject content = CreateElement("Content", viewport);
        RectTransform contentRt = content.GetComponent<RectTransform>();
        contentRt.anchorMin = new Vector2(0f, 1f);
        contentRt.anchorMax = new Vector2(1f, 1f);
        contentRt.pivot = new Vector2(0.5f, 1f);
        contentRt.anchoredPosition = Vector2.zero;
        contentRt.sizeDelta = new Vector2(0f, 0f);

        VerticalLayoutGroup layout = content.AddComponent<VerticalLayoutGroup>();
        layout.spacing = spacing;
        layout.padding = new RectOffset(0, 0, 0, 0);
        layout.childControlWidth = true;
        layout.childControlHeight = true;
        layout.childForceExpandWidth = true;
        layout.childForceExpandHeight = false;

        ContentSizeFitter fitter = content.AddComponent<ContentSizeFitter>();
        fitter.verticalFit = ContentSizeFitter.FitMode.PreferredSize;

        scrollRect.viewport = viewport.GetComponent<RectTransform>();
        scrollRect.content = contentRt;

        return contentRt;
    }

    private static void CreateTableHeader(GameObject parent, HeaderColumn[] columns, Vector2 anchorMin, Vector2 anchorMax, Vector2 anchoredPosition, float height)
    {
        GameObject header = CreatePanelCard("HeaderRow", parent, new Color(1f, 1f, 1f, 0.05f), Clear);
        RectTransform headerRt = header.GetComponent<RectTransform>();
        headerRt.anchorMin = anchorMin;
        headerRt.anchorMax = anchorMax;
        headerRt.pivot = new Vector2(0.5f, 1f);
        headerRt.sizeDelta = new Vector2(0f, height);
        headerRt.anchoredPosition = anchoredPosition;

        HorizontalLayoutGroup layout = header.AddComponent<HorizontalLayoutGroup>();
        layout.padding = new RectOffset(16, 16, 8, 8);
        layout.spacing = 10f;
        layout.childAlignment = TextAnchor.MiddleLeft;
        layout.childControlWidth = true;
        layout.childControlHeight = true;
        layout.childForceExpandHeight = false;
        layout.childForceExpandWidth = false;

        for (int i = 0; i < columns.Length; i++)
        {
            CreateCellText(header, $"HeaderText{i}", columns[i].label, columns[i].width, TextAlignmentOptions.Left, FontWeight.Medium, TextSecondary);
        }
    }

    private readonly struct HeaderColumn
    {
        public readonly string label;
        public readonly float width;

        public HeaderColumn(string label, float width)
        {
            this.label = label;
            this.width = width;
        }
    }

    private static GameObject CreateRowTemplate(string name, GameObject parent, float height)
    {
        GameObject row = CreatePanelCard(name, parent, RowBg, Clear);
        LayoutElement layoutElement = row.AddComponent<LayoutElement>();
        layoutElement.preferredHeight = height;
        layoutElement.flexibleWidth = 1f;

        HorizontalLayoutGroup layout = row.AddComponent<HorizontalLayoutGroup>();
        layout.padding = new RectOffset(14, 14, 8, 8);
        layout.spacing = 10f;
        layout.childControlWidth = true;
        layout.childControlHeight = true;
        layout.childForceExpandHeight = false;
        layout.childForceExpandWidth = false;
        layout.childAlignment = TextAnchor.MiddleLeft;

        return row;
    }

    private static TextMeshProUGUI CreateCellText(GameObject parent, string name, string text, float width, TextAlignmentOptions alignment = TextAlignmentOptions.Left, FontWeight fontWeight = FontWeight.Regular, Color? color = null)
    {
        GameObject wrapper = CreateElement(name + "Wrap", parent);
        LayoutElement layout = wrapper.AddComponent<LayoutElement>();
        if (width > 0f)
        {
            layout.preferredWidth = width;
            layout.minWidth = width;
        }
        else
        {
            layout.flexibleWidth = 1f;
        }

        TextMeshProUGUI tmp = CreateText(name, wrapper, text, 13f, fontWeight, color ?? TextPrimary, alignment);
        StretchFull(tmp.GetComponent<RectTransform>());
        return tmp;
    }

    private static void CreateStatusCell(GameObject parent, string badgeName, string textName, float width)
    {
        GameObject wrapper = CreateElement(textName + "Wrap", parent);
        HorizontalLayoutGroup layout = wrapper.AddComponent<HorizontalLayoutGroup>();
        layout.spacing = 8f;
        layout.childAlignment = TextAnchor.MiddleLeft;
        layout.childControlWidth = true;
        layout.childControlHeight = true;
        layout.childForceExpandHeight = false;
        layout.childForceExpandWidth = false;

        LayoutElement element = wrapper.AddComponent<LayoutElement>();
        element.preferredWidth = width;
        element.minWidth = width;

        Image badge = CreatePanelCard(badgeName, wrapper, new Color(0.15f, 0.68f, 0.38f, 0.14f), Clear).GetComponent<Image>();
        RectTransform badgeRt = badge.GetComponent<RectTransform>();
        badgeRt.sizeDelta = new Vector2(18f, 18f);

        CreateText(textName, wrapper, "正常", 12f, FontWeight.Medium, TextPrimary, TextAlignmentOptions.Left);
    }

    private static TextMeshProUGUI CreateInfoRow(GameObject parent, string valueName, string labelText, string valueText)
    {
        GameObject row = CreateElement(valueName + "Row", parent);
        LayoutElement layoutElement = row.AddComponent<LayoutElement>();
        layoutElement.preferredHeight = 22f;

        HorizontalLayoutGroup layout = row.AddComponent<HorizontalLayoutGroup>();
        layout.spacing = 10f;
        layout.childControlWidth = true;
        layout.childControlHeight = true;
        layout.childForceExpandHeight = false;
        layout.childForceExpandWidth = false;
        layout.childAlignment = TextAnchor.MiddleLeft;

        CreateCellText(row, valueName + "Label", labelText, 84f, TextAlignmentOptions.Left, FontWeight.Medium, TextSecondary);
        return CreateCellText(row, valueName, valueText, -1f, TextAlignmentOptions.Left, FontWeight.Regular, TextPrimary);
    }

    private static TMP_InputField CreateCompactInputField(GameObject parent, string name, string placeholder, Vector2 size)
    {
        TMP_InputField input = CreateInputControl(name, parent, placeholder, size, false);
        return input;
    }

    private static TMP_Dropdown CreateCompactDropdown(GameObject parent, string name, List<string> options, Vector2 size)
    {
        TMP_Dropdown dropdown = CreateDropdownControl(name, parent, size, options.Count > 0 ? options[0] : "请选择");
        dropdown.ClearOptions();
        dropdown.AddOptions(options);
        return dropdown;
    }

    private static TMP_InputField CreateLabeledInputField(GameObject parent, string name, string label, string placeholder, float height, bool multiLine)
    {
        GameObject wrapper = CreateElement(name + "Wrapper", parent);
        LayoutElement layout = wrapper.AddComponent<LayoutElement>();
        layout.preferredHeight = height;
        layout.flexibleWidth = 1f;

        VerticalLayoutGroup group = wrapper.AddComponent<VerticalLayoutGroup>();
        group.spacing = 6f;
        group.childControlWidth = true;
        group.childControlHeight = true;
        group.childForceExpandHeight = false;
        group.childForceExpandWidth = true;

        TextMeshProUGUI labelText = CreateText("FieldLabel", wrapper, label, 12f, FontWeight.Medium, TextSecondary, TextAlignmentOptions.Left);
        labelText.rectTransform.sizeDelta = new Vector2(0f, 20f);

        TMP_InputField input = CreateInputControl(name, wrapper, placeholder, new Vector2(0f, multiLine ? height - 30f : 44f), multiLine);
        LayoutElement inputLayout = input.gameObject.AddComponent<LayoutElement>();
        inputLayout.preferredHeight = multiLine ? height - 30f : 44f;
        inputLayout.flexibleWidth = 1f;
        return input;
    }

    private static TMP_Dropdown CreateLabeledDropdownField(GameObject parent, string name, string label, List<string> options, float height)
    {
        GameObject wrapper = CreateElement(name + "Wrapper", parent);
        LayoutElement layout = wrapper.AddComponent<LayoutElement>();
        layout.preferredHeight = height;
        layout.flexibleWidth = 1f;

        VerticalLayoutGroup group = wrapper.AddComponent<VerticalLayoutGroup>();
        group.spacing = 6f;
        group.childControlWidth = true;
        group.childControlHeight = true;
        group.childForceExpandHeight = false;
        group.childForceExpandWidth = true;

        TextMeshProUGUI labelText = CreateText("FieldLabel", wrapper, label, 12f, FontWeight.Medium, TextSecondary, TextAlignmentOptions.Left);
        labelText.rectTransform.sizeDelta = new Vector2(0f, 20f);

        TMP_Dropdown dropdown = CreateDropdownControl(name, wrapper, new Vector2(0f, 44f), options.Count > 0 ? options[0] : label);
        dropdown.ClearOptions();
        dropdown.AddOptions(options);
        LayoutElement dropLayout = dropdown.gameObject.AddComponent<LayoutElement>();
        dropLayout.preferredHeight = 44f;
        dropLayout.flexibleWidth = 1f;
        return dropdown;
    }

    private static TMP_InputField CreateInputControl(string name, GameObject parent, string placeholder, Vector2 size, bool multiLine)
    {
        GameObject inputGo = CreatePanelCard(name, parent, InputBg, InputBorder);
        RectTransform rt = inputGo.GetComponent<RectTransform>();
        rt.sizeDelta = size;

        TMP_InputField input = inputGo.AddComponent<TMP_InputField>();

        GameObject textArea = CreateElement("Text Area", inputGo);
        RectTransform textAreaRt = textArea.GetComponent<RectTransform>();
        textAreaRt.anchorMin = Vector2.zero;
        textAreaRt.anchorMax = Vector2.one;
        textAreaRt.offsetMin = new Vector2(14f, 8f);
        textAreaRt.offsetMax = new Vector2(-14f, -8f);
        textArea.AddComponent<RectMask2D>();

        TextMeshProUGUI placeholderText = CreateText("Placeholder", textArea, placeholder, 13f, FontWeight.Regular, TextMuted, multiLine ? TextAlignmentOptions.TopLeft : TextAlignmentOptions.Left);
        StretchFull(placeholderText.GetComponent<RectTransform>());
        placeholderText.enableWordWrapping = multiLine;

        TextMeshProUGUI inputText = CreateText("Text", textArea, string.Empty, 13f, FontWeight.Regular, TextPrimary, multiLine ? TextAlignmentOptions.TopLeft : TextAlignmentOptions.Left);
        StretchFull(inputText.GetComponent<RectTransform>());
        inputText.enableWordWrapping = multiLine;

        input.textViewport = textAreaRt;
        input.textComponent = inputText;
        input.placeholder = placeholderText;
        input.lineType = multiLine ? TMP_InputField.LineType.MultiLineNewline : TMP_InputField.LineType.SingleLine;

        return input;
    }

    private static TMP_Dropdown CreateDropdownControl(string name, GameObject parent, Vector2 size, string caption)
    {
        GameObject dropdownGo = CreatePanelCard(name, parent, InputBg, InputBorder);
        RectTransform rt = dropdownGo.GetComponent<RectTransform>();
        rt.sizeDelta = size;

        TMP_Dropdown dropdown = dropdownGo.AddComponent<TMP_Dropdown>();
        dropdown.targetGraphic = dropdownGo.GetComponent<Image>();

        TextMeshProUGUI label = CreateText("Label", dropdownGo, caption, 13f, FontWeight.Regular, TextPrimary, TextAlignmentOptions.Left);
        RectTransform labelRt = label.GetComponent<RectTransform>();
        labelRt.anchorMin = new Vector2(0f, 0f);
        labelRt.anchorMax = new Vector2(0.86f, 1f);
        labelRt.offsetMin = new Vector2(14f, 0f);
        labelRt.offsetMax = new Vector2(0f, 0f);
        dropdown.captionText = label;

        TextMeshProUGUI arrow = CreateText("Arrow", dropdownGo, "▼", 10f, FontWeight.Medium, TextSecondary, TextAlignmentOptions.Center);
        RectTransform arrowRt = arrow.GetComponent<RectTransform>();
        arrowRt.anchorMin = new Vector2(0.86f, 0.2f);
        arrowRt.anchorMax = new Vector2(1f, 0.8f);
        arrowRt.offsetMin = Vector2.zero;
        arrowRt.offsetMax = new Vector2(-10f, 0f);

        GameObject template = CreatePanelCard("Template", dropdownGo, CardBg, PanelBorder);
        RectTransform templateRt = template.GetComponent<RectTransform>();
        templateRt.anchorMin = new Vector2(0f, 0f);
        templateRt.anchorMax = new Vector2(1f, 0f);
        templateRt.pivot = new Vector2(0.5f, 1f);
        templateRt.sizeDelta = new Vector2(0f, 240f);
        templateRt.anchoredPosition = new Vector2(0f, -4f);

        ScrollRect scroll = template.AddComponent<ScrollRect>();
        scroll.horizontal = false;
        scroll.vertical = true;
        scroll.movementType = ScrollRect.MovementType.Clamped;

        GameObject viewport = CreateElement("Viewport", template);
        StretchFull(viewport.GetComponent<RectTransform>());
        Image viewportImg = viewport.AddComponent<Image>();
        ApplyDefaultSprite(viewportImg);
        viewportImg.color = new Color(0f, 0f, 0f, 0f);
        viewport.AddComponent<Mask>().showMaskGraphic = false;

        GameObject content = CreateElement("Content", viewport);
        RectTransform contentRt = content.GetComponent<RectTransform>();
        contentRt.anchorMin = new Vector2(0f, 1f);
        contentRt.anchorMax = new Vector2(1f, 1f);
        contentRt.pivot = new Vector2(0.5f, 1f);
        contentRt.anchoredPosition = Vector2.zero;
        contentRt.sizeDelta = new Vector2(0f, 30f);

        scroll.viewport = viewport.GetComponent<RectTransform>();
        scroll.content = contentRt;

        GameObject item = CreateElement("Item", content);
        RectTransform itemRt = item.GetComponent<RectTransform>();
        itemRt.anchorMin = new Vector2(0f, 0.5f);
        itemRt.anchorMax = new Vector2(1f, 0.5f);
        itemRt.pivot = new Vector2(0.5f, 0.5f);
        itemRt.anchoredPosition = Vector2.zero;
        itemRt.sizeDelta = new Vector2(0f, 30f);
        LayoutElement itemLe = item.AddComponent<LayoutElement>();
        itemLe.minHeight = 30f;
        itemLe.preferredHeight = 30f;
        itemLe.flexibleWidth = 1f;
        Toggle itemToggle = item.AddComponent<Toggle>();

        GameObject itemBg = CreateElement("Item Background", item);
        StretchFull(itemBg.GetComponent<RectTransform>());
        Image itemBgImage = itemBg.AddComponent<Image>();
        ApplyDefaultSprite(itemBgImage);
        itemBgImage.color = Clear;
        itemToggle.targetGraphic = itemBgImage;

        GameObject itemCheckmark = CreateElement("Item Checkmark", itemBg);
        RectTransform itemCheckmarkRt = itemCheckmark.GetComponent<RectTransform>();
        itemCheckmarkRt.anchorMin = new Vector2(0f, 0f);
        itemCheckmarkRt.anchorMax = new Vector2(0f, 1f);
        itemCheckmarkRt.pivot = new Vector2(0f, 0.5f);
        itemCheckmarkRt.sizeDelta = new Vector2(14f, 0f);
        itemCheckmarkRt.anchoredPosition = new Vector2(10f, 0f);
        Image itemCheckImage = itemCheckmark.AddComponent<Image>();
        ApplyDefaultSprite(itemCheckImage);
        itemCheckImage.color = AccentBlue;
        itemToggle.graphic = itemCheckImage;

        TextMeshProUGUI itemLabel = CreateText("Item Label", itemBg, caption, 13f, FontWeight.Regular, TextPrimary, TextAlignmentOptions.Left);
        RectTransform itemLabelRt = itemLabel.GetComponent<RectTransform>();
        itemLabelRt.anchorMin = new Vector2(0f, 0f);
        itemLabelRt.anchorMax = new Vector2(1f, 1f);
        itemLabelRt.offsetMin = new Vector2(28f, 0f);
        itemLabelRt.offsetMax = new Vector2(-10f, 0f);

        dropdown.template = templateRt;
        dropdown.itemText = itemLabel;
        dropdown.itemImage = itemCheckImage;
        template.SetActive(false);

        return dropdown;
    }

    private static Slider CreateSlider(GameObject parent, string name, Vector2 size, Color fillColor)
    {
        GameObject sliderGo = CreateElement(name, parent);
        RectTransform rt = sliderGo.GetComponent<RectTransform>();
        rt.sizeDelta = size;

        Slider slider = sliderGo.AddComponent<Slider>();
        slider.minValue = 0f;
        slider.maxValue = 1f;
        slider.value = 0.85f;
        slider.direction = Slider.Direction.LeftToRight;

        GameObject background = CreatePanelCard("Background", sliderGo, new Color(1f, 1f, 1f, 0.1f), Clear);
        StretchFull(background.GetComponent<RectTransform>());

        GameObject fillArea = CreateElement("Fill Area", sliderGo);
        RectTransform fillAreaRt = fillArea.GetComponent<RectTransform>();
        fillAreaRt.anchorMin = new Vector2(0f, 0f);
        fillAreaRt.anchorMax = new Vector2(1f, 1f);
        fillAreaRt.offsetMin = new Vector2(2f, 2f);
        fillAreaRt.offsetMax = new Vector2(-2f, -2f);

        GameObject fill = CreatePanelCard("Fill", fillArea, fillColor, Clear);
        RectTransform fillRt = fill.GetComponent<RectTransform>();
        fillRt.anchorMin = new Vector2(0f, 0f);
        fillRt.anchorMax = new Vector2(1f, 1f);
        fillRt.offsetMin = Vector2.zero;
        fillRt.offsetMax = Vector2.zero;

        slider.fillRect = fillRt;
        slider.targetGraphic = background.GetComponent<Image>();
        return slider;
    }

    private static GameObject CreateElement(string name, GameObject parent)
    {
        GameObject go = new GameObject(name, typeof(RectTransform));
        if (parent != null)
            go.transform.SetParent(parent.transform, false);
        return go;
    }

    private static TextMeshProUGUI CreateText(string name, GameObject parent, string text, float fontSize, FontWeight fontWeight, Color color, TextAlignmentOptions alignment)
    {
        GameObject go = CreateElement(name, parent);
        TextMeshProUGUI tmp = go.AddComponent<TextMeshProUGUI>();
        tmp.font = ResolveFont();
        tmp.text = text;
        tmp.fontSize = fontSize;
        tmp.fontWeight = fontWeight;
        tmp.color = color;
        tmp.alignment = alignment;
        tmp.raycastTarget = false;
        tmp.enableWordWrapping = false;
        return tmp;
    }

    private static Button CreateButton(string name, GameObject parent, string label, float fontSize, FontWeight fontWeight, Color textColor, Color backgroundColor, Vector2 size)
    {
        GameObject buttonGo = CreateElement(name, parent);
        RectTransform rt = buttonGo.GetComponent<RectTransform>();
        rt.sizeDelta = size;

        Image image = buttonGo.AddComponent<Image>();
        ApplyDefaultSprite(image);
        image.color = backgroundColor;

        Button button = buttonGo.AddComponent<Button>();
        button.targetGraphic = image;

        ColorBlock colors = button.colors;
        colors.normalColor = backgroundColor;
        colors.highlightedColor = Multiply(backgroundColor, 1.08f);
        colors.pressedColor = Multiply(backgroundColor, 0.9f);
        colors.selectedColor = backgroundColor;
        colors.disabledColor = new Color(backgroundColor.r, backgroundColor.g, backgroundColor.b, 0.4f);
        button.colors = colors;

        TextMeshProUGUI labelText = CreateText("Label", buttonGo, label, fontSize, fontWeight, textColor, TextAlignmentOptions.Center);
        StretchFull(labelText.GetComponent<RectTransform>());

        return button;
    }

    private static GameObject CreatePanelCard(string name, GameObject parent, Color backgroundColor, Color borderColor)
    {
        GameObject go = CreateElement(name, parent);
        Image image = go.AddComponent<Image>();
        ApplyDefaultSprite(image);
        image.color = backgroundColor;

        if (borderColor.a > 0f)
        {
            Outline outline = go.AddComponent<Outline>();
            outline.effectColor = borderColor;
            outline.effectDistance = new Vector2(1f, 1f);
        }

        return go;
    }

    private static void CreateSeparator(GameObject parent, float anchoredY)
    {
        GameObject separator = CreateElement("Separator", parent);
        RectTransform rt = separator.GetComponent<RectTransform>();
        rt.anchorMin = new Vector2(0f, 1f);
        rt.anchorMax = new Vector2(1f, 1f);
        rt.pivot = new Vector2(0.5f, 1f);
        rt.sizeDelta = new Vector2(0f, 1f);
        rt.anchoredPosition = new Vector2(0f, anchoredY);
        Image image = separator.AddComponent<Image>();
        ApplyDefaultSprite(image);
        image.color = new Color(1f, 1f, 1f, 0.08f);
    }

    private static void StretchFull(RectTransform rt)
    {
        rt.anchorMin = Vector2.zero;
        rt.anchorMax = Vector2.one;
        rt.offsetMin = Vector2.zero;
        rt.offsetMax = Vector2.zero;
    }

    private static void SetRect(RectTransform rt, Vector2 anchorMin, Vector2 anchorMax, Vector2 pivot, Vector2 sizeDelta, Vector2 anchoredPosition)
    {
        rt.anchorMin = anchorMin;
        rt.anchorMax = anchorMax;
        rt.pivot = pivot;
        rt.sizeDelta = sizeDelta;
        rt.anchoredPosition = anchoredPosition;
    }

    private static TMP_FontAsset ResolveFont()
    {
        if (cachedFont != null)
            return cachedFont;

        cachedFont = AssetDatabase.LoadAssetAtPath<TMP_FontAsset>(ChineseFontAssetPath);
        if (cachedFont == null)
            cachedFont = TMP_Settings.defaultFontAsset;
        if (cachedFont == null)
            Debug.LogError("[ReagentAndEquipmentUIBuilder] 无法找到可用字体资源。");
        return cachedFont;
    }

    private static void ApplyDefaultSprite(Image image)
    {
        if (image == null)
            return;

        if (cachedUiSprite == null)
        {
            cachedUiSprite = AssetDatabase.GetBuiltinExtraResource<Sprite>("UI/Skin/UISprite.psd");
            if (cachedUiSprite == null)
                cachedUiSprite = AssetDatabase.GetBuiltinExtraResource<Sprite>("UI/Skin/Background.psd");
        }

        image.sprite = cachedUiSprite;
        image.type = Image.Type.Sliced;
        image.preserveAspect = false;
    }

    private static GameObject FindSceneObjectByName(string name)
    {
        GameObject[] allObjects = Resources.FindObjectsOfTypeAll<GameObject>();
        foreach (GameObject go in allObjects)
        {
            if (go != null && go.scene.IsValid() && go.name == name)
                return go;
        }

        return null;
    }

    private static void DestroyIfExists(string name)
    {
        GameObject existing = FindSceneObjectByName(name);
        if (existing != null)
            Object.DestroyImmediate(existing);
    }

    private static Color Multiply(Color color, float factor)
    {
        return new Color(
            Mathf.Clamp01(color.r * factor),
            Mathf.Clamp01(color.g * factor),
            Mathf.Clamp01(color.b * factor),
            color.a);
    }

    private static Color Hex(string hex)
    {
        ColorUtility.TryParseHtmlString(hex, out Color color);
        return color;
    }
}