using System.Collections.Generic;
using TMPro;
using UnityEditor;
using UnityEngine;
using UnityEngine.UI;

public class LabInfoPanelBuilder : EditorWindow
{
    private const string ChineseFontAssetPath = "Assets/字体/ZiHunBingLin.asset";

    private static TMP_FontAsset cachedFont;
    private static Sprite cachedUiSprite;

    private static readonly Color AccentBlue = Hex("#5B9BD5");
    private static readonly Color AccentBlueSoft = Hex("#78B4E8");
    private static readonly Color SuccessGreen = Hex("#2ECC71");
    private static readonly Color WarningOrange = Hex("#F39C12");
    private static readonly Color TextPrimary = Hex("#F3F5F7");
    private static readonly Color TextSecondary = Hex("#B6BDC9");
    private static readonly Color TextMuted = Hex("#7A8394");
    private static readonly Color OverlayBg = new Color(0f, 0f, 0f, 0.68f);
    private static readonly Color PanelBg = Hex("#1B2230");
    private static readonly Color PanelBgAlt = Hex("#20293A");
    private static readonly Color PanelOutline = Hex("#313D53");
    private static readonly Color CardBg = Hex("#222C3F");
    private static readonly Color CardBorder = Hex("#38445B");
    private static readonly Color ListRowBg = Hex("#3A5068");
    private static readonly Color ListRowBorder = Hex("#5A7A9E");
    private static readonly Color OverviewCardBg = Hex("#3B4F6B");
    private static readonly Color OverviewCardHeader = Hex("#2C3B50");
    private static readonly Color OverviewCardFooter = Hex("#314257");
    private static readonly Color OverviewCardBorder = Hex("#6B8BB8");
    private static readonly Color OverviewLocationText = Hex("#F2F6FC");
    private static readonly Color OverviewHintText = Hex("#9FD4FF");
    private static readonly Color InputBg = Hex("#141B28");
    private static readonly Color InputBorder = Hex("#334258");
    private static readonly Color SeparatorLine = new Color(1f, 1f, 1f, 0.07f);
    private static readonly Color TabInactive = Hex("#243048");
    private static readonly Color TabActive = Hex("#172131");
    private static readonly Color White = Color.white;

    [MenuItem("Tools/搭建大屏交互 UI", false, 100)]
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
        EnsureLabDataProvider();
        EnsureEventSystem();
        EnsureIMEHandler();

        GameObject panelRoot = BuildLabInfoPanel();
        AutoBindReferences(panelRoot);

        Selection.activeGameObject = panelRoot;

        EditorUtility.DisplayDialog(
            "搭建完成",
            "Class_Lab 大屏 UI 已按深色主题重新生成。\n\n请重新打开场景中的大屏面板查看效果。",
            "确定");
    }

    [MenuItem("Tools/应用当前选中看板卡样式", false, 101)]
    public static void RestyleSelectedOverviewCard()
    {
        GameObject selected = Selection.activeGameObject;
        if (selected == null)
        {
            EditorUtility.DisplayDialog("未选中对象", "请先在 Hierarchy 中选中一张概览卡或其子节点。", "确定");
            return;
        }

        Transform cardRoot = FindOverviewCardRoot(selected.transform);
        if (cardRoot == null)
        {
            EditorUtility.DisplayDialog("不是概览卡", "当前选中对象不在 OverviewPanel 的 CardContainer 下。", "确定");
            return;
        }

        ApplyOverviewCardStyleToExisting(cardRoot.gameObject);
        EditorUtility.SetDirty(cardRoot.gameObject);
        Canvas.ForceUpdateCanvases();
        LayoutRebuilder.ForceRebuildLayoutImmediate(cardRoot.GetComponent<RectTransform>());
        Selection.activeGameObject = cardRoot.gameObject;

        EditorUtility.DisplayDialog("应用完成", "当前选中的概览卡已切换为高对比半透明黑底样式，并已同步到概览卡预制体。", "确定");
    }

    private static TMP_FontAsset ResolveFont()
    {
        if (cachedFont != null)
        {
            return cachedFont;
        }

        // 删除旧版损坏的动态字体
        string brokenPath = "Assets/字体/ZiHunBingLin_Dynamic.asset";
        if (AssetDatabase.LoadAssetAtPath<TMP_FontAsset>(brokenPath) != null)
        {
            AssetDatabase.DeleteAsset(brokenPath);
            AssetDatabase.Refresh();
        }

        cachedFont = AssetDatabase.LoadAssetAtPath<TMP_FontAsset>(ChineseFontAssetPath);
        if (cachedFont == null)
        {
            cachedFont = TMP_Settings.defaultFontAsset;
        }

        if (cachedFont == null)
        {
            Debug.LogError("[LabInfoPanelBuilder] 无法找到任何可用字体！");
        }

        return cachedFont;
    }

    private static void EnsureLabDataProvider()
    {
        // GameObject.Find 只能找 active 对象，用 FindObjectsOfTypeAll 包含 inactive
        LabDataProvider[] existing = Resources.FindObjectsOfTypeAll<LabDataProvider>();
        foreach (var p in existing)
        {
            // 过滤掉 Prefab 资产（只要场景内实例）
            if (p.gameObject.scene.IsValid())
                return;
        }

        GameObject go = new GameObject("LabDataProvider");
        go.AddComponent<LabDataProvider>();
    }

    private static void EnsureEventSystem()
    {
        if (Object.FindObjectOfType<UnityEngine.EventSystems.EventSystem>() != null)
        {
            return;
        }

        GameObject go = new GameObject("EventSystem");
        go.AddComponent<UnityEngine.EventSystems.EventSystem>();
        go.AddComponent<UnityEngine.EventSystems.StandaloneInputModule>();
    }

    private static void EnsureIMEHandler()
    {
        if (Object.FindObjectOfType<WebGLIMEHandler>() != null)
            return;

        GameObject imeGo = new GameObject("IMEHandler");
        imeGo.AddComponent<WebGLIMEHandler>();
        Debug.Log("[LabInfoPanelBuilder] 已自动创建 WebGLIMEHandler");
    }

    private static GameObject BuildLabInfoPanel()
    {
        GameObject oldPanel = GameObject.Find("LabInfoPanel");
        if (oldPanel != null)
        {
            Object.DestroyImmediate(oldPanel);
        }

        GameObject root = CreateElement("LabInfoPanel", null);

        Canvas canvas = root.AddComponent<Canvas>();
        canvas.renderMode = RenderMode.ScreenSpaceOverlay;
        canvas.sortingOrder = 100;

        CanvasScaler scaler = root.AddComponent<CanvasScaler>();
        scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
        scaler.referenceResolution = new Vector2(1920f, 1080f);
        scaler.matchWidthOrHeight = 0.5f;

        root.AddComponent<GraphicRaycaster>();
        LabInfoPanelUI panelUI = root.AddComponent<LabInfoPanelUI>();

        GameObject overlay = CreateElement("OverlayBg", root);
        RectTransform overlayRt = overlay.GetComponent<RectTransform>();
        StretchFull(overlayRt);
        overlay.AddComponent<Image>().color = OverlayBg;
        Button overlayButton = overlay.AddComponent<Button>();
        overlayButton.transition = Selectable.Transition.None;

        GameObject mainPanel = CreatePanelCard("MainPanel", overlay, PanelBg, PanelOutline);
        RectTransform mainRt = mainPanel.GetComponent<RectTransform>();
        mainRt.anchorMin = new Vector2(0.5f, 0.5f);
        mainRt.anchorMax = new Vector2(0.5f, 0.5f);
        mainRt.pivot = new Vector2(0.5f, 0.5f);
        mainRt.sizeDelta = new Vector2(1260f, 820f);
        mainRt.anchoredPosition = Vector2.zero;

        BuildHeader(mainPanel);
        BuildTabs(mainPanel);
        GameObject overviewPanel = BuildOverviewPanel(mainPanel);
        GameObject borrowPanel = BuildBorrowPanel(mainPanel);
        borrowPanel.SetActive(false);

        GameObject loadingOverlay = CreateElement("LoadingOverlay", overlay);
        StretchFull(loadingOverlay.GetComponent<RectTransform>());
        loadingOverlay.AddComponent<Image>().color = new Color(0f, 0f, 0f, 0.52f);
        loadingOverlay.SetActive(false);

        TextMeshProUGUI loadingText = CreateText(
            "LoadingText",
            loadingOverlay,
            "加载中...",
            18,
            FontWeight.Medium,
            TextPrimary,
            TextAlignmentOptions.Center);
        RectTransform loadingTextRt = loadingText.GetComponent<RectTransform>();
        loadingTextRt.anchorMin = new Vector2(0.5f, 0.5f);
        loadingTextRt.anchorMax = new Vector2(0.5f, 0.5f);
        loadingTextRt.pivot = new Vector2(0.5f, 0.5f);
        loadingTextRt.sizeDelta = new Vector2(260f, 60f);
        loadingTextRt.anchoredPosition = Vector2.zero;

        Transform cardContainer = overviewPanel.transform.Find("ScrollView/Viewport/CardContainer");
        TextMeshProUGUI summaryText = overviewPanel.transform.Find("SummaryCard/OverviewSummaryText")?.GetComponent<TextMeshProUGUI>();

        GameObject templateRoot = CreateElement("TemplateRoot", root);
        templateRoot.SetActive(false);

        GameObject cardTemplate = BuildCardTemplate(templateRoot);
        cardTemplate.name = "CardTemplate";
        cardTemplate.SetActive(false);

        PopulateOverviewPreviewCards(cardContainer, cardTemplate, summaryText);

        panelUI.tabActiveColor = TabActive;
        panelUI.tabInactiveColor = TabInactive;

        overlayButton.onClick.AddListener(panelUI.ClosePanel);
        AutoConnectToScreenTrigger(root);
        root.SetActive(true);

        return root;
    }

    private static void BuildHeader(GameObject mainPanel)
    {
        GameObject header = CreateElement("HeaderBar", mainPanel);
        RectTransform headerRt = header.GetComponent<RectTransform>();
        headerRt.anchorMin = new Vector2(0f, 1f);
        headerRt.anchorMax = new Vector2(1f, 1f);
        headerRt.pivot = new Vector2(0.5f, 1f);
        headerRt.sizeDelta = new Vector2(0f, 76f);
        headerRt.anchoredPosition = new Vector2(0f, -12f);

        TextMeshProUGUI title = CreateText(
            "TitleText",
            header,
            "教学实验室 · 信息展示大屏",
            24,
            FontWeight.Bold,
            TextPrimary,
            TextAlignmentOptions.Left);
        RectTransform titleRt = title.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 0f);
        titleRt.anchorMax = new Vector2(0.8f, 1f);
        titleRt.offsetMin = new Vector2(34f, 10f);
        titleRt.offsetMax = new Vector2(0f, 0f);

        TextMeshProUGUI subTitle = CreateText(
            "SubTitleText",
            header,
            "展示同类型实验室的多间房间信息，并支持预约借用。",
            12,
            FontWeight.Regular,
            TextSecondary,
            TextAlignmentOptions.Left);
        RectTransform subTitleRt = subTitle.GetComponent<RectTransform>();
        subTitleRt.anchorMin = new Vector2(0f, 0f);
        subTitleRt.anchorMax = new Vector2(0.82f, 1f);
        subTitleRt.offsetMin = new Vector2(36f, -18f);
        subTitleRt.offsetMax = new Vector2(0f, -26f);

        Button closeButton = CreateButton(
            "CloseButton",
            header,
            "X",
            18,
            FontWeight.Bold,
            TextSecondary,
            Hex("#303D52"),
            new Vector2(38f, 38f));
        RectTransform closeRt = closeButton.GetComponent<RectTransform>();
        closeRt.anchorMin = new Vector2(1f, 0.5f);
        closeRt.anchorMax = new Vector2(1f, 0.5f);
        closeRt.pivot = new Vector2(1f, 0.5f);
        closeRt.anchoredPosition = new Vector2(-22f, 0f);

        CreateSeparator("HeaderSeparator", mainPanel, -78f);
    }

    private static void BuildTabs(GameObject mainPanel)
    {
        GameObject tabBar = CreateElement("TabBar", mainPanel);
        RectTransform tabBarRt = tabBar.GetComponent<RectTransform>();
        tabBarRt.anchorMin = new Vector2(0f, 1f);
        tabBarRt.anchorMax = new Vector2(1f, 1f);
        tabBarRt.pivot = new Vector2(0.5f, 1f);
        tabBarRt.sizeDelta = new Vector2(0f, 52f);
        tabBarRt.anchoredPosition = new Vector2(0f, -88f);

        Button overviewTab = CreateButton(
            "TabOverview",
            tabBar,
            "实验室展示",
            15,
            FontWeight.SemiBold,
            TextPrimary,
            TabActive,
            new Vector2(212f, 40f));
        RectTransform overviewTabRt = overviewTab.GetComponent<RectTransform>();
        overviewTabRt.anchorMin = new Vector2(0f, 0.5f);
        overviewTabRt.anchorMax = new Vector2(0f, 0.5f);
        overviewTabRt.pivot = new Vector2(0f, 0.5f);
        overviewTabRt.anchoredPosition = new Vector2(28f, 0f);
        CreateTabHighlight("TabOverviewHighlight", overviewTab.gameObject, true);

        Button borrowTab = CreateButton(
            "TabBorrow",
            tabBar,
            "借用申请",
            15,
            FontWeight.SemiBold,
            TextSecondary,
            TabInactive,
            new Vector2(212f, 40f));
        RectTransform borrowTabRt = borrowTab.GetComponent<RectTransform>();
        borrowTabRt.anchorMin = new Vector2(0f, 0.5f);
        borrowTabRt.anchorMax = new Vector2(0f, 0.5f);
        borrowTabRt.pivot = new Vector2(0f, 0.5f);
        borrowTabRt.anchoredPosition = new Vector2(252f, 0f);
        CreateTabHighlight("TabBorrowHighlight", borrowTab.gameObject, false);

        CreateSeparator("TabSeparator", mainPanel, -138f);
    }

    private static GameObject BuildOverviewPanel(GameObject mainPanel)
    {
        GameObject panel = CreateElement("OverviewPanel", mainPanel);
        RectTransform panelRt = panel.GetComponent<RectTransform>();
        panelRt.anchorMin = new Vector2(0f, 0f);
        panelRt.anchorMax = new Vector2(1f, 1f);
        panelRt.offsetMin = new Vector2(28f, 28f);
        panelRt.offsetMax = new Vector2(-28f, -152f);

        TextMeshProUGUI sectionTitle = CreateText(
            "SectionTitleText",
            panel,
            "同类型实验室一览",
            18,
            FontWeight.SemiBold,
            TextPrimary,
            TextAlignmentOptions.Left);
        RectTransform sectionTitleRt = sectionTitle.GetComponent<RectTransform>();
        sectionTitleRt.anchorMin = new Vector2(0f, 1f);
        sectionTitleRt.anchorMax = new Vector2(0.4f, 1f);
        sectionTitleRt.pivot = new Vector2(0f, 1f);
        sectionTitleRt.sizeDelta = new Vector2(0f, 32f);
        sectionTitleRt.anchoredPosition = new Vector2(0f, 0f);

        TextMeshProUGUI sectionHint = CreateText(
            "SectionHintText",
            panel,
            "主界面仅展示名称与地点，点击卡片可查看详细信息并进入借用申请。",
            12,
            FontWeight.Regular,
            TextSecondary,
            TextAlignmentOptions.Right);
        RectTransform sectionHintRt = sectionHint.GetComponent<RectTransform>();
        sectionHintRt.anchorMin = new Vector2(0.5f, 1f);
        sectionHintRt.anchorMax = new Vector2(1f, 1f);
        sectionHintRt.pivot = new Vector2(1f, 1f);
        sectionHintRt.sizeDelta = new Vector2(0f, 28f);
        sectionHintRt.anchoredPosition = new Vector2(0f, -2f);

        GameObject summaryCard = CreatePanelCard("SummaryCard", panel, PanelBgAlt, CardBorder);
        RectTransform summaryCardRt = summaryCard.GetComponent<RectTransform>();
        summaryCardRt.anchorMin = new Vector2(0f, 1f);
        summaryCardRt.anchorMax = new Vector2(1f, 1f);
        summaryCardRt.pivot = new Vector2(0.5f, 1f);
        summaryCardRt.sizeDelta = new Vector2(0f, 64f);
        summaryCardRt.anchoredPosition = new Vector2(0f, -42f);

        TextMeshProUGUI summaryText = CreateText(
            "OverviewSummaryText",
            summaryCard,
            "共 5 间 · 开放中 3 · 维护中 1 · 已关闭 1",
            15,
            FontWeight.Medium,
            TextPrimary,
            TextAlignmentOptions.Left);
        RectTransform summaryTextRt = summaryText.GetComponent<RectTransform>();
        summaryTextRt.anchorMin = new Vector2(0f, 0f);
        summaryTextRt.anchorMax = new Vector2(1f, 1f);
        summaryTextRt.offsetMin = new Vector2(20f, 0f);
        summaryTextRt.offsetMax = new Vector2(-20f, 0f);

        GameObject scrollView = CreateElement("ScrollView", panel);
        RectTransform scrollViewRt = scrollView.GetComponent<RectTransform>();
        scrollViewRt.anchorMin = new Vector2(0f, 0f);
        scrollViewRt.anchorMax = new Vector2(1f, 1f);
        scrollViewRt.offsetMin = new Vector2(0f, 0f);
        scrollViewRt.offsetMax = new Vector2(0f, -120f);

        Image scrollViewImg = scrollView.AddComponent<Image>();
        scrollViewImg.color = new Color(0f, 0f, 0f, 0f);
        scrollViewImg.raycastTarget = true;

        ScrollRect scrollRect = scrollView.AddComponent<ScrollRect>();
        scrollRect.horizontal = false;
        scrollRect.vertical = true;
        scrollRect.movementType = ScrollRect.MovementType.Clamped;
        scrollRect.scrollSensitivity = 28f;

        GameObject viewport = CreateElement("Viewport", scrollView);
        RectTransform viewportRt = viewport.GetComponent<RectTransform>();
        StretchFull(viewportRt);
        // 使用 RectMask2D 替代 Mask：不依赖 Stencil Buffer，在编辑器模式下更可靠
        viewport.AddComponent<RectMask2D>();

        GameObject cardContainer = CreateElement("CardContainer", viewport);
        RectTransform cardContainerRt = cardContainer.GetComponent<RectTransform>();
        cardContainerRt.anchorMin = new Vector2(0f, 1f);
        cardContainerRt.anchorMax = new Vector2(1f, 1f);
        cardContainerRt.pivot = new Vector2(0.5f, 1f);
        cardContainerRt.sizeDelta = new Vector2(0f, 0f);
        cardContainerRt.anchoredPosition = Vector2.zero;

        VerticalLayoutGroup vlg = cardContainer.AddComponent<VerticalLayoutGroup>();
        vlg.spacing = 8f;
        vlg.padding = new RectOffset(4, 4, 4, 4);
        vlg.childForceExpandWidth = true;
        vlg.childForceExpandHeight = false;
        vlg.childControlWidth = true;
        vlg.childControlHeight = true;

        ContentSizeFitter contentSizeFitter = cardContainer.AddComponent<ContentSizeFitter>();
        contentSizeFitter.horizontalFit = ContentSizeFitter.FitMode.Unconstrained;
        contentSizeFitter.verticalFit = ContentSizeFitter.FitMode.PreferredSize;

        scrollRect.viewport = viewportRt;
        scrollRect.content = cardContainerRt;

        BuildOverviewDetailOverlay(panel);

        return panel;
    }

    private static void BuildOverviewDetailOverlay(GameObject panel)
    {
        GameObject overlay = CreateElement("DetailOverlay", panel);
        RectTransform overlayRt = overlay.GetComponent<RectTransform>();
        StretchFull(overlayRt);
        overlay.AddComponent<Image>().color = new Color(0f, 0f, 0f, 0.62f);
        Button overlayButton = overlay.AddComponent<Button>();
        overlayButton.transition = Selectable.Transition.None;
        overlay.SetActive(false);

        GameObject modal = CreatePanelCard("DetailModal", overlay, PanelBg, PanelOutline);
        RectTransform modalRt = modal.GetComponent<RectTransform>();
        modalRt.anchorMin = new Vector2(0.5f, 0.5f);
        modalRt.anchorMax = new Vector2(0.5f, 0.5f);
        modalRt.pivot = new Vector2(0.5f, 0.5f);
        modalRt.sizeDelta = new Vector2(760f, 470f);
        modalRt.anchoredPosition = Vector2.zero;

        TextMeshProUGUI title = CreateText(
            "DetailLabNameText",
            modal,
            "A301 教学实验室",
            25,
            FontWeight.Bold,
            TextPrimary,
            TextAlignmentOptions.Left);
        RectTransform titleRt = title.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(0.7f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 34f);
        titleRt.anchoredPosition = new Vector2(26f, -22f);

        TextMeshProUGUI code = CreateText(
            "DetailLabCodeText",
            modal,
            "LAB-2024-A301",
            12,
            FontWeight.Regular,
            TextMuted,
            TextAlignmentOptions.Left);
        RectTransform codeRt = code.GetComponent<RectTransform>();
        codeRt.anchorMin = new Vector2(0f, 1f);
        codeRt.anchorMax = new Vector2(0.6f, 1f);
        codeRt.pivot = new Vector2(0f, 1f);
        codeRt.sizeDelta = new Vector2(0f, 20f);
        codeRt.anchoredPosition = new Vector2(28f, -58f);

        GameObject statusBadge = CreateElement("DetailStatusBadge", modal);
        RectTransform statusBadgeRt = statusBadge.GetComponent<RectTransform>();
        statusBadgeRt.anchorMin = new Vector2(1f, 1f);
        statusBadgeRt.anchorMax = new Vector2(1f, 1f);
        statusBadgeRt.pivot = new Vector2(1f, 1f);
        statusBadgeRt.sizeDelta = new Vector2(88f, 30f);
        statusBadgeRt.anchoredPosition = new Vector2(-24f, -24f);
        statusBadge.AddComponent<Image>().color = SuccessGreen;

        TextMeshProUGUI statusText = CreateText(
            "DetailStatusText",
            statusBadge,
            "开放中",
            12,
            FontWeight.SemiBold,
            White,
            TextAlignmentOptions.Center);
        StretchFull(statusText.GetComponent<RectTransform>());

        TextMeshProUGUI location = CreateText(
            "DetailLocationText",
            modal,
            "东校区 · 教学楼A 3F A301",
            14,
            FontWeight.Medium,
            TextSecondary,
            TextAlignmentOptions.Left);
        RectTransform locationRt = location.GetComponent<RectTransform>();
        locationRt.anchorMin = new Vector2(0f, 1f);
        locationRt.anchorMax = new Vector2(1f, 1f);
        locationRt.pivot = new Vector2(0f, 1f);
        locationRt.sizeDelta = new Vector2(0f, 22f);
        locationRt.anchoredPosition = new Vector2(28f, -92f);

        CreateSeparator("DetailSeparator", modal, -120f);

        TextMeshProUGUI descTitle = CreateText(
            "DetailDescriptionTitleText",
            modal,
            "实验室简介",
            14,
            FontWeight.SemiBold,
            AccentBlueSoft,
            TextAlignmentOptions.Left);
        RectTransform descTitleRt = descTitle.GetComponent<RectTransform>();
        descTitleRt.anchorMin = new Vector2(0f, 1f);
        descTitleRt.anchorMax = new Vector2(1f, 1f);
        descTitleRt.pivot = new Vector2(0f, 1f);
        descTitleRt.sizeDelta = new Vector2(0f, 24f);
        descTitleRt.anchoredPosition = new Vector2(28f, -138f);

        TextMeshProUGUI description = CreateText(
            "DetailDescriptionText",
            modal,
            "标准教学实验环境，配备常规实验器材与教学演示设备。",
            13,
            FontWeight.Regular,
            TextPrimary,
            TextAlignmentOptions.TopLeft);
        RectTransform descriptionRt = description.GetComponent<RectTransform>();
        descriptionRt.anchorMin = new Vector2(0f, 1f);
        descriptionRt.anchorMax = new Vector2(1f, 1f);
        descriptionRt.pivot = new Vector2(0f, 1f);
        descriptionRt.sizeDelta = new Vector2(0f, 72f);
        descriptionRt.anchoredPosition = new Vector2(28f, -168f);
        description.enableWordWrapping = true;

        CreateDetailMetricCard(modal, "CapacityMetricCard", "容纳人数", "DetailCapacityText", "40 人", new Vector2(28f, -254f));
        CreateDetailMetricCard(modal, "AreaMetricCard", "使用面积", "DetailAreaText", "85 m²", new Vector2(272f, -254f));
        CreateDetailMetricCard(modal, "DutyMetricCard", "负责人", "DetailDutyPersonText", "张教授", new Vector2(516f, -254f));

        TextMeshProUGUI equipTitle = CreateText(
            "DetailEquipmentTitleText",
            modal,
            "主要设备",
            14,
            FontWeight.SemiBold,
            AccentBlueSoft,
            TextAlignmentOptions.Left);
        RectTransform equipTitleRt = equipTitle.GetComponent<RectTransform>();
        equipTitleRt.anchorMin = new Vector2(0f, 1f);
        equipTitleRt.anchorMax = new Vector2(1f, 1f);
        equipTitleRt.pivot = new Vector2(0f, 1f);
        equipTitleRt.sizeDelta = new Vector2(0f, 24f);
        equipTitleRt.anchoredPosition = new Vector2(28f, -354f);

        TextMeshProUGUI equipment = CreateText(
            "DetailEquipmentText",
            modal,
            "多功能实验台×20、投影仪×1、白板×2",
            13,
            FontWeight.Regular,
            TextSecondary,
            TextAlignmentOptions.TopLeft);
        RectTransform equipmentRt = equipment.GetComponent<RectTransform>();
        equipmentRt.anchorMin = new Vector2(0f, 1f);
        equipmentRt.anchorMax = new Vector2(1f, 1f);
        equipmentRt.pivot = new Vector2(0f, 1f);
        equipmentRt.sizeDelta = new Vector2(0f, 42f);
        equipmentRt.anchoredPosition = new Vector2(28f, -384f);
        equipment.enableWordWrapping = true;

        Button closeButton = CreateButton(
            "DetailCloseButton",
            modal,
            "关闭详情",
            14,
            FontWeight.Medium,
            TextSecondary,
            Hex("#273145"),
            new Vector2(118f, 42f));
        RectTransform closeRt = closeButton.GetComponent<RectTransform>();
        closeRt.anchorMin = new Vector2(1f, 0f);
        closeRt.anchorMax = new Vector2(1f, 0f);
        closeRt.pivot = new Vector2(1f, 0f);
        closeRt.anchoredPosition = new Vector2(-180f, 20f);

        Button borrowButton = CreateButton(
            "DetailBorrowButton",
            modal,
            "申请借用",
            14,
            FontWeight.SemiBold,
            White,
            AccentBlue,
            new Vector2(138f, 42f));
        RectTransform borrowRt = borrowButton.GetComponent<RectTransform>();
        borrowRt.anchorMin = new Vector2(1f, 0f);
        borrowRt.anchorMax = new Vector2(1f, 0f);
        borrowRt.pivot = new Vector2(1f, 0f);
        borrowRt.anchoredPosition = new Vector2(-28f, 20f);
    }

    private static GameObject BuildBorrowPanel(GameObject mainPanel)
    {
        GameObject panel = CreateElement("BorrowPanel", mainPanel);
        RectTransform panelRt = panel.GetComponent<RectTransform>();
        panelRt.anchorMin = new Vector2(0f, 0f);
        panelRt.anchorMax = new Vector2(1f, 1f);
        panelRt.offsetMin = new Vector2(28f, 28f);
        panelRt.offsetMax = new Vector2(-28f, -152f);

        LabBorrowFormUI formController = panel.AddComponent<LabBorrowFormUI>();

        TextMeshProUGUI title = CreateText(
            "BorrowPanelTitleText",
            panel,
            "实验室借用申请",
            18,
            FontWeight.SemiBold,
            TextPrimary,
            TextAlignmentOptions.Left);
        RectTransform titleRt = title.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(0.5f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 32f);
        titleRt.anchoredPosition = new Vector2(0f, 0f);

        TextMeshProUGUI intro = CreateText(
            "BorrowPanelIntroText",
            panel,
            "提交后进入审批流程，建议填写清晰的用途、人数与时间信息。",
            12,
            FontWeight.Regular,
            TextSecondary,
            TextAlignmentOptions.Left);
        RectTransform introRt = intro.GetComponent<RectTransform>();
        introRt.anchorMin = new Vector2(0f, 1f);
        introRt.anchorMax = new Vector2(1f, 1f);
        introRt.pivot = new Vector2(0f, 1f);
        introRt.sizeDelta = new Vector2(0f, 28f);
        introRt.anchoredPosition = new Vector2(0f, -26f);

        GameObject leftCard = CreatePanelCard("BorrowInfoCard", panel, PanelBgAlt, CardBorder);
        RectTransform leftCardRt = leftCard.GetComponent<RectTransform>();
        leftCardRt.anchorMin = new Vector2(0f, 0f);
        leftCardRt.anchorMax = new Vector2(0.34f, 1f);
        leftCardRt.offsetMin = new Vector2(0f, 0f);
        leftCardRt.offsetMax = new Vector2(-12f, -64f);

        TextMeshProUGUI leftTitle = CreateText(
            "InfoTitleText",
            leftCard,
            "借用流程与说明",
            16,
            FontWeight.SemiBold,
            TextPrimary,
            TextAlignmentOptions.Left);
        RectTransform leftTitleRt = leftTitle.GetComponent<RectTransform>();
        leftTitleRt.anchorMin = new Vector2(0f, 1f);
        leftTitleRt.anchorMax = new Vector2(1f, 1f);
        leftTitleRt.pivot = new Vector2(0f, 1f);
        leftTitleRt.sizeDelta = new Vector2(0f, 28f);
        leftTitleRt.anchoredPosition = new Vector2(20f, -20f);

        CreateInfoChip(leftCard, "线上提交", AccentBlue, new Vector2(20f, -62f));
        CreateInfoChip(leftCard, "管理员审核", SuccessGreen, new Vector2(132f, -62f));
        CreateInfoChip(leftCard, "使用可追踪", WarningOrange, new Vector2(256f, -62f));

        CreateBullet(leftCard, "1. 选择同类型实验室并填写预约时间。", new Vector2(22f, -116f));
        CreateBullet(leftCard, "2. 补充用途、人数与申请人信息。", new Vector2(22f, -156f));
        CreateBullet(leftCard, "3. 提交后等待管理员审批结果。", new Vector2(22f, -196f));
        CreateBullet(leftCard, "建议优先选择开放中的实验室，维护中房间不可预约。", new Vector2(22f, -250f), 2);

        GameObject noteBox = CreatePanelCard("PreviewNoteCard", leftCard, CardBg, CardBorder);
        RectTransform noteBoxRt = noteBox.GetComponent<RectTransform>();
        noteBoxRt.anchorMin = new Vector2(0f, 0f);
        noteBoxRt.anchorMax = new Vector2(1f, 0f);
        noteBoxRt.pivot = new Vector2(0.5f, 0f);
        noteBoxRt.sizeDelta = new Vector2(0f, 132f);
        noteBoxRt.anchoredPosition = new Vector2(0f, 18f);

        TextMeshProUGUI noteTitle = CreateText(
            "PreviewNoteTitleText",
            noteBox,
            "当前展示类型",
            13,
            FontWeight.SemiBold,
            AccentBlueSoft,
            TextAlignmentOptions.Left);
        RectTransform noteTitleRt = noteTitle.GetComponent<RectTransform>();
        noteTitleRt.anchorMin = new Vector2(0f, 1f);
        noteTitleRt.anchorMax = new Vector2(1f, 1f);
        noteTitleRt.pivot = new Vector2(0f, 1f);
        noteTitleRt.sizeDelta = new Vector2(0f, 22f);
        noteTitleRt.anchoredPosition = new Vector2(16f, -14f);

        TextMeshProUGUI noteText = CreateText(
            "PreviewNoteBodyText",
            noteBox,
            "本场景将集中展示“教学实验室”这一类别下的多间房间信息。\n借用申请会默认针对当前类别下的实验室资源进行提交。",
            12,
            FontWeight.Regular,
            TextSecondary,
            TextAlignmentOptions.TopLeft);
        RectTransform noteTextRt = noteText.GetComponent<RectTransform>();
        noteTextRt.anchorMin = new Vector2(0f, 0f);
        noteTextRt.anchorMax = new Vector2(1f, 1f);
        noteTextRt.offsetMin = new Vector2(16f, 16f);
        noteTextRt.offsetMax = new Vector2(-16f, -40f);
        noteText.enableWordWrapping = true;

        GameObject formCard = CreatePanelCard("BorrowFormCard", panel, PanelBgAlt, CardBorder);
        RectTransform formCardRt = formCard.GetComponent<RectTransform>();
        formCardRt.anchorMin = new Vector2(0.38f, 0f);
        formCardRt.anchorMax = new Vector2(1f, 1f);
        formCardRt.offsetMin = new Vector2(12f, 0f);
        formCardRt.offsetMax = new Vector2(0f, -64f);

        TextMeshProUGUI formTitle = CreateText(
            "FormTitle",
            formCard,
            "填写借用信息",
            16,
            FontWeight.SemiBold,
            TextPrimary,
            TextAlignmentOptions.Left);
        RectTransform formTitleRt = formTitle.GetComponent<RectTransform>();
        formTitleRt.anchorMin = new Vector2(0f, 1f);
        formTitleRt.anchorMax = new Vector2(1f, 1f);
        formTitleRt.pivot = new Vector2(0f, 1f);
        formTitleRt.sizeDelta = new Vector2(0f, 28f);
        formTitleRt.anchoredPosition = new Vector2(22f, -20f);

        GameObject formBody = CreateElement("FormBody", formCard);
        RectTransform formBodyRt = formBody.GetComponent<RectTransform>();
        formBodyRt.anchorMin = new Vector2(0f, 0f);
        formBodyRt.anchorMax = new Vector2(1f, 1f);
        formBodyRt.offsetMin = new Vector2(20f, 22f);
        formBodyRt.offsetMax = new Vector2(-20f, -58f);

        VerticalLayoutGroup formBodyLayout = formBody.AddComponent<VerticalLayoutGroup>();
        formBodyLayout.spacing = 14f;
        formBodyLayout.childForceExpandWidth = true;
        formBodyLayout.childForceExpandHeight = false;
        formBodyLayout.childControlWidth = true;
        formBodyLayout.childControlHeight = false;

        GameObject labField = CreateDropdownField(formBody, "LabDropdown", "目标实验室");
        GameObject rowOne = CreateFieldRow(formBody, "ApplicantRow");
        GameObject applicantField = CreateInputField(rowOne, "ApplicantNameInput", "申请人姓名");
        GameObject peopleField = CreateInputField(rowOne, "PeopleCountInput", "使用人数");
        peopleField.GetComponent<TMP_InputField>().contentType = TMP_InputField.ContentType.IntegerNumber;

        GameObject rowTwo = CreateFieldRow(formBody, "TimeRow");
        GameObject dateField = CreateInputField(rowTwo, "DateInput", "预约日期 (YYYY-MM-DD)");
        GameObject timeField = CreateDropdownField(rowTwo, "TimeSlotDropdown", "预约时段");

        GameObject purposeField = CreateInputField(formBody, "PurposeInput", "用途说明", 120f, true);

        GameObject buttonRow = CreateElement("ButtonRow", formBody);
        RectTransform buttonRowRt = buttonRow.GetComponent<RectTransform>();
        buttonRowRt.sizeDelta = new Vector2(0f, 44f);
        HorizontalLayoutGroup buttonLayout = buttonRow.AddComponent<HorizontalLayoutGroup>();
        buttonLayout.spacing = 14f;
        buttonLayout.childForceExpandWidth = false;
        buttonLayout.childForceExpandHeight = true;
        buttonLayout.childAlignment = TextAnchor.MiddleRight;

        Button clearButton = CreateButton(
            "ClearButton",
            buttonRow,
            "清空内容",
            14,
            FontWeight.Medium,
            TextSecondary,
            Hex("#273145"),
            new Vector2(118f, 42f));
        Button submitButton = CreateButton(
            "SubmitButton",
            buttonRow,
            "提交申请",
            14,
            FontWeight.SemiBold,
            White,
            AccentBlue,
            new Vector2(138f, 42f));

        TextMeshProUGUI statusMessage = CreateText(
            "StatusMessage",
            formBody,
            string.Empty,
            13,
            FontWeight.Regular,
            SuccessGreen,
            TextAlignmentOptions.Left);
        RectTransform statusMessageRt = statusMessage.GetComponent<RectTransform>();
        statusMessageRt.sizeDelta = new Vector2(0f, 24f);
        statusMessage.gameObject.SetActive(false);

        GameObject successOverlay = CreateElement("SuccessOverlay", panel);
        StretchFull(successOverlay.GetComponent<RectTransform>());
        successOverlay.AddComponent<Image>().color = new Color(0.18f, 0.8f, 0.44f, 0.12f);
        successOverlay.SetActive(false);
        TextMeshProUGUI successText = CreateText(
            "SuccessText",
            successOverlay,
            "预约申请已提交，请等待管理员审核！",
            18,
            FontWeight.SemiBold,
            SuccessGreen,
            TextAlignmentOptions.Center);
        RectTransform successTextRt = successText.GetComponent<RectTransform>();
        successTextRt.anchorMin = new Vector2(0.5f, 0.5f);
        successTextRt.anchorMax = new Vector2(0.5f, 0.5f);
        successTextRt.pivot = new Vector2(0.5f, 0.5f);
        successTextRt.sizeDelta = new Vector2(460f, 56f);
        successTextRt.anchoredPosition = Vector2.zero;

        TMP_Dropdown labDropdown = labField.GetComponent<TMP_Dropdown>();
        TMP_InputField applicantInput = applicantField.GetComponent<TMP_InputField>();
        TMP_InputField dateInput = dateField.GetComponent<TMP_InputField>();
        TMP_Dropdown timeDropdown = timeField.GetComponent<TMP_Dropdown>();
        TMP_InputField peopleInput = peopleField.GetComponent<TMP_InputField>();
        TMP_InputField purposeInput = purposeField.GetComponent<TMP_InputField>();

        timeDropdown.ClearOptions();
        timeDropdown.AddOptions(new List<string>
        {
            "08:00 - 10:00",
            "10:00 - 12:00",
            "14:00 - 16:00",
            "16:00 - 18:00",
            "18:00 - 20:00",
            "20:00 - 22:00"
        });

        formController.labDropdown = labDropdown;
        formController.applicantNameInput = applicantInput;
        formController.dateInput = dateInput;
        formController.timeSlotDropdown = timeDropdown;
        formController.peopleCountInput = peopleInput;
        formController.purposeInput = purposeInput;
        formController.submitButton = submitButton;
        formController.clearButton = clearButton;
        formController.statusMessage = statusMessage;
        formController.successOverlay = successOverlay;
        formController.successText = successText;

        EditorUtility.SetDirty(formController);

        return panel;
    }

    private static GameObject BuildCardTemplate(GameObject overviewPanel)
    {
        // 列表行模板：左边名称+地点+状态，右边查看按钮
        GameObject row = CreatePanelCard("LabListRow_Template", overviewPanel, ListRowBg, ListRowBorder);
        RectTransform rowRt = row.GetComponent<RectTransform>();
        rowRt.sizeDelta = new Vector2(0f, 64f);

        LayoutElement rowLe = row.AddComponent<LayoutElement>();
        rowLe.minHeight = 64f;
        rowLe.preferredHeight = 64f;

        HorizontalLayoutGroup hlg = row.AddComponent<HorizontalLayoutGroup>();
        hlg.spacing = 16f;
        hlg.padding = new RectOffset(16, 12, 8, 8);
        hlg.childForceExpandWidth = false;
        hlg.childForceExpandHeight = true;
        hlg.childControlWidth = false;
        hlg.childControlHeight = true;
        hlg.childAlignment = TextAnchor.MiddleLeft;

        // 左侧：状态色块
        GameObject statusDot = CreateElement("StatusBadge", row);
        RectTransform dotRt = statusDot.GetComponent<RectTransform>();
        dotRt.sizeDelta = new Vector2(12f, 12f);
        Image dotImage = statusDot.AddComponent<Image>();
        dotImage.color = SuccessGreen;
        dotImage.raycastTarget = false;

        // 左侧：名称 + 地点/状态 竖排
        GameObject infoGroup = CreateElement("InfoGroup", row);
        RectTransform infoRt = infoGroup.GetComponent<RectTransform>();
        infoRt.sizeDelta = new Vector2(0f, 48f);
        LayoutElement infoLe = infoGroup.AddComponent<LayoutElement>();
        infoLe.flexibleWidth = 1f;

        VerticalLayoutGroup infoVlg = infoGroup.AddComponent<VerticalLayoutGroup>();
        infoVlg.spacing = 4f;
        infoVlg.childForceExpandWidth = true;
        infoVlg.childForceExpandHeight = false;
        infoVlg.childControlWidth = true;
        infoVlg.childControlHeight = false;

        TextMeshProUGUI labName = CreateText(
            "LabNameText", infoGroup,
            "实验室名称", 16, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        labName.rectTransform.sizeDelta = new Vector2(0f, 22f);

        // 地点 + 状态 横排
        GameObject subRow = CreateElement("SubRow", infoGroup);
        subRow.GetComponent<RectTransform>().sizeDelta = new Vector2(0f, 20f);
        HorizontalLayoutGroup subHlg = subRow.AddComponent<HorizontalLayoutGroup>();
        subHlg.spacing = 12f;
        subHlg.childForceExpandWidth = false;
        subHlg.childForceExpandHeight = true;
        subHlg.childControlWidth = false;
        subHlg.childControlHeight = true;

        TextMeshProUGUI locationText = CreateText(
            "LocationText", subRow,
            "东校区 · 教学楼A 3F", 12, FontWeight.Regular, TextSecondary, TextAlignmentOptions.Left);
        locationText.rectTransform.sizeDelta = new Vector2(360f, 20f);

        TextMeshProUGUI statusText = CreateText(
            "StatusText", subRow,
            "开放中", 12, FontWeight.Medium, SuccessGreen, TextAlignmentOptions.Left);
        statusText.rectTransform.sizeDelta = new Vector2(60f, 20f);

        // 右侧：查看按钮
        Button viewButton = CreateButton(
            "ViewDetailButton", row,
            "查看详情", 13, FontWeight.SemiBold, White, AccentBlue,
            new Vector2(90f, 36f));

        return row;
    }

    private static void CreateDetailMetricCard(GameObject parent, string name, string title, string valueTargetName, string value, Vector2 anchoredPosition)
    {
        GameObject card = CreatePanelCard(name, parent, CardBg, CardBorder);
        RectTransform cardRt = card.GetComponent<RectTransform>();
        cardRt.anchorMin = new Vector2(0f, 1f);
        cardRt.anchorMax = new Vector2(0f, 1f);
        cardRt.pivot = new Vector2(0f, 1f);
        cardRt.sizeDelta = new Vector2(216f, 76f);
        cardRt.anchoredPosition = anchoredPosition;

        TextMeshProUGUI titleText = CreateText(
            "Title",
            card,
            title,
            12,
            FontWeight.Regular,
            TextMuted,
            TextAlignmentOptions.Left);
        RectTransform titleRt = titleText.GetComponent<RectTransform>();
        titleRt.anchorMin = new Vector2(0f, 1f);
        titleRt.anchorMax = new Vector2(1f, 1f);
        titleRt.pivot = new Vector2(0f, 1f);
        titleRt.sizeDelta = new Vector2(0f, 20f);
        titleRt.anchoredPosition = new Vector2(16f, -14f);

        TextMeshProUGUI valueText = CreateText(
            valueTargetName,
            card,
            value,
            19,
            FontWeight.SemiBold,
            TextPrimary,
            TextAlignmentOptions.Left);
        RectTransform valueRt = valueText.GetComponent<RectTransform>();
        valueRt.anchorMin = new Vector2(0f, 0f);
        valueRt.anchorMax = new Vector2(1f, 1f);
        valueRt.offsetMin = new Vector2(16f, 10f);
        valueRt.offsetMax = new Vector2(-16f, -24f);
    }

    private static void PopulateOverviewPreviewCards(Transform cardContainer, GameObject cardTemplate, TextMeshProUGUI summaryText)
    {
        if (cardContainer == null)
        {
            Debug.LogError("[Builder] CardContainer 为 null");
            return;
        }

        foreach (Transform child in cardContainer)
        {
            Object.DestroyImmediate(child.gameObject);
        }

        // FindObjectOfType 只找 active 对象，用 FindObjectsOfTypeAll 包含 inactive
        LabDataProvider provider = null;
        foreach (var p in Resources.FindObjectsOfTypeAll<LabDataProvider>())
        {
            if (p.gameObject.scene.IsValid()) { provider = p; break; }
        }
        if (provider == null)
        {
            Debug.LogError("[Builder] 找不到 LabDataProvider");
            return;
        }

        if (provider.mockLabData == null || provider.mockLabData.Count == 0)
        {
            provider.GenerateMockDataPublic();
        }

        List<LabInfoData> labs = provider.GetLabsByType(provider.currentSceneLabType);
        if (labs == null || labs.Count == 0)
        {
            Debug.LogWarning("[Builder] 没有实验室数据");
            return;
        }

        Debug.Log($"[Builder] 开始生成 {labs.Count} 个列表行...");

        int total = labs.Count;
        int openCount = 0, maintainCount = 0, closedCount = 0;
        foreach (var l in labs)
        {
            if (l.runningStatus == 1) openCount++;
            else if (l.runningStatus == 2) maintainCount++;
            else closedCount++;
        }

        if (summaryText != null)
            summaryText.text = $"共 <color=#4A90D9>{total}</color> 间 · 开放中 <color=#27AE60>{openCount}</color> · 维护中 <color=#F39C12>{maintainCount}</color> · 已关闭 <color=#E74C3C>{closedCount}</color>";

        Sprite sprite = ResolveUiSprite();
        TMP_FontAsset font = ResolveFont();

        foreach (LabInfoData lab in labs)
        {
            Color statusColor = SuccessGreen;
            ColorUtility.TryParseHtmlString(lab.StatusColor, out statusColor);

            // ---- 行根：深色卡片，左侧彩色状态条 ----
            GameObject row = new GameObject($"Preview_{lab.labCode}",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(LayoutElement));
            row.transform.SetParent(cardContainer, false);

            Image rowImg = row.GetComponent<Image>();
            rowImg.sprite = sprite;
            rowImg.color = Hex("#1E2D42");   // 深蓝底色，更精致
            rowImg.type = Image.Type.Simple;
            rowImg.raycastTarget = true;

            LayoutElement rowLe = row.GetComponent<LayoutElement>();
            rowLe.minHeight = 72f;
            rowLe.preferredHeight = 72f;
            rowLe.flexibleWidth = 1f;

            // ---- 左侧状态色条（4px 宽竖条） ----
            GameObject accent = new GameObject("AccentBar",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(LayoutElement));
            accent.transform.SetParent(row.transform, false);
            RectTransform accentRt = accent.GetComponent<RectTransform>();
            accentRt.anchorMin = new Vector2(0f, 0f);
            accentRt.anchorMax = new Vector2(0f, 1f);
            accentRt.pivot = new Vector2(0f, 0.5f);
            accentRt.sizeDelta = new Vector2(4f, 0f);
            accentRt.anchoredPosition = Vector2.zero;
            Image accentImg = accent.GetComponent<Image>();
            accentImg.sprite = sprite;
            accentImg.color = statusColor;
            accentImg.raycastTarget = false;
            accent.GetComponent<LayoutElement>().ignoreLayout = true;

            // ---- 内容 HLG（左缩进留出色条位置） ----
            GameObject content = new GameObject("RowContent",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(HorizontalLayoutGroup));
            content.transform.SetParent(row.transform, false);
            RectTransform contentRt = content.GetComponent<RectTransform>();
            contentRt.anchorMin = Vector2.zero;
            contentRt.anchorMax = Vector2.one;
            contentRt.offsetMin = new Vector2(14f, 0f);  // 左缩进（色条占 4px，再留 10px）
            contentRt.offsetMax = new Vector2(-16f, 0f);

            HorizontalLayoutGroup contentHlg = content.GetComponent<HorizontalLayoutGroup>();
            contentHlg.spacing = 14f;
            contentHlg.padding = new RectOffset(0, 0, 0, 0);
            contentHlg.childForceExpandWidth = false;
            contentHlg.childForceExpandHeight = true;
            contentHlg.childControlWidth = true;
            contentHlg.childControlHeight = true;
            contentHlg.childAlignment = TextAnchor.MiddleLeft;

            // ---- 状态圆点 ----
            GameObject dot = new GameObject("StatusBadge",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(LayoutElement));
            dot.transform.SetParent(content.transform, false);
            LayoutElement dotLe = dot.GetComponent<LayoutElement>();
            dotLe.minWidth = 10f;
            dotLe.preferredWidth = 10f;
            dotLe.minHeight = 10f;
            dotLe.preferredHeight = 10f;
            dotLe.flexibleWidth = 0f;
            Image dotImg = dot.GetComponent<Image>();
            dotImg.sprite = sprite;
            dotImg.color = statusColor;
            dotImg.raycastTarget = false;

            // ---- 文字信息区（占满剩余空间） ----
            GameObject info = new GameObject("InfoGroup",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(LayoutElement), typeof(VerticalLayoutGroup));
            info.transform.SetParent(content.transform, false);
            LayoutElement infoLe = info.GetComponent<LayoutElement>();
            infoLe.flexibleWidth = 1f;
            infoLe.minWidth = 100f;

            VerticalLayoutGroup infoVlg = info.GetComponent<VerticalLayoutGroup>();
            infoVlg.spacing = 4f;
            infoVlg.padding = new RectOffset(0, 0, 12, 12);
            infoVlg.childForceExpandWidth = true;
            infoVlg.childForceExpandHeight = false;
            infoVlg.childControlWidth = true;
            infoVlg.childControlHeight = true;
            infoVlg.childAlignment = TextAnchor.UpperLeft;

            // 实验室名称
            GameObject nameGo = new GameObject("LabNameText",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI), typeof(LayoutElement));
            nameGo.transform.SetParent(info.transform, false);
            nameGo.GetComponent<LayoutElement>().preferredHeight = 22f;
            TextMeshProUGUI nameTmp = nameGo.GetComponent<TextMeshProUGUI>();
            nameTmp.font = font;
            nameTmp.text = lab.labName;
            nameTmp.fontSize = 15;
            nameTmp.fontWeight = FontWeight.SemiBold;
            nameTmp.color = TextPrimary;
            nameTmp.alignment = TextAlignmentOptions.Left;
            nameTmp.overflowMode = TextOverflowModes.Ellipsis;
            nameTmp.raycastTarget = false;

            // 地点行（图标 + 地点文字 + 分隔符 + 编号）
            GameObject subRow = new GameObject("SubRow",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(HorizontalLayoutGroup), typeof(LayoutElement));
            subRow.transform.SetParent(info.transform, false);
            subRow.GetComponent<LayoutElement>().preferredHeight = 18f;
            HorizontalLayoutGroup subHlg = subRow.GetComponent<HorizontalLayoutGroup>();
            subHlg.spacing = 6f;
            subHlg.childForceExpandWidth = false;
            subHlg.childForceExpandHeight = true;
            subHlg.childControlWidth = true;
            subHlg.childControlHeight = true;
            subHlg.childAlignment = TextAnchor.MiddleLeft;

            // 地点文字
            GameObject locGo = new GameObject("LocationText",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI), typeof(LayoutElement));
            locGo.transform.SetParent(subRow.transform, false);
            locGo.GetComponent<LayoutElement>().flexibleWidth = 1f;
            TextMeshProUGUI locTmp = locGo.GetComponent<TextMeshProUGUI>();
            locTmp.font = font;
            locTmp.text = lab.FullLocation;
            locTmp.fontSize = 11;
            locTmp.fontWeight = FontWeight.Regular;
            locTmp.color = TextSecondary;
            locTmp.alignment = TextAlignmentOptions.Left;
            locTmp.overflowMode = TextOverflowModes.Ellipsis;
            locTmp.raycastTarget = false;

            // 分隔点
            GameObject sepGo = new GameObject("Sep",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI), typeof(LayoutElement));
            sepGo.transform.SetParent(subRow.transform, false);
            sepGo.GetComponent<LayoutElement>().preferredWidth = 10f;
            TextMeshProUGUI sepTmp = sepGo.GetComponent<TextMeshProUGUI>();
            sepTmp.font = font;
            sepTmp.text = "·";
            sepTmp.fontSize = 11;
            sepTmp.color = TextMuted;
            sepTmp.alignment = TextAlignmentOptions.Center;
            sepTmp.raycastTarget = false;

            // 编号文字
            GameObject codeGo = new GameObject("LabCodeText",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI), typeof(LayoutElement));
            codeGo.transform.SetParent(subRow.transform, false);
            codeGo.GetComponent<LayoutElement>().preferredWidth = 120f;
            TextMeshProUGUI codeTmp = codeGo.GetComponent<TextMeshProUGUI>();
            codeTmp.font = font;
            codeTmp.text = lab.labCode;
            codeTmp.fontSize = 11;
            codeTmp.color = TextMuted;
            codeTmp.alignment = TextAlignmentOptions.Left;
            codeTmp.raycastTarget = false;

            // ---- 状态徽章（右侧） ----
            GameObject badge = new GameObject("StatusBadgePill",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(LayoutElement), typeof(HorizontalLayoutGroup));
            badge.transform.SetParent(content.transform, false);
            LayoutElement badgeLe = badge.GetComponent<LayoutElement>();
            badgeLe.preferredWidth = 64f;
            badgeLe.minWidth = 64f;
            badgeLe.flexibleWidth = 0f;
            Image badgeImg = badge.GetComponent<Image>();
            badgeImg.sprite = sprite;
            // 状态色半透明背景
            badgeImg.color = new Color(statusColor.r, statusColor.g, statusColor.b, 0.18f);
            badgeImg.type = Image.Type.Simple;
            badgeImg.raycastTarget = false;
            HorizontalLayoutGroup badgeHlg = badge.GetComponent<HorizontalLayoutGroup>();
            badgeHlg.childAlignment = TextAnchor.MiddleCenter;
            badgeHlg.childControlWidth = true;
            badgeHlg.childControlHeight = true;
            badgeHlg.childForceExpandWidth = false;
            badgeHlg.childForceExpandHeight = false;

            GameObject stGo = new GameObject("StatusText",
                typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI));
            stGo.transform.SetParent(badge.transform, false);
            TextMeshProUGUI stTmp = stGo.GetComponent<TextMeshProUGUI>();
            stTmp.font = font;
            stTmp.text = lab.StatusName;
            stTmp.fontSize = 11;
            stTmp.fontWeight = FontWeight.Medium;
            stTmp.color = statusColor;
            stTmp.alignment = TextAlignmentOptions.Center;
            stTmp.raycastTarget = false;

            // ---- 查看详情按钮（固定宽度，靠右） ----
            // 先添加分隔用 LayoutElement
            GameObject spacer = new GameObject("BtnSpacer", typeof(RectTransform), typeof(LayoutElement));
            spacer.transform.SetParent(content.transform, false);
            LayoutElement spacerLe = spacer.GetComponent<LayoutElement>();
            spacerLe.preferredWidth = 0f;
            spacerLe.flexibleWidth = 0f;
            spacerLe.minWidth = 0f;

            Button viewBtn = CreateButton("ViewDetailButton", content, "查看详情",
                12, FontWeight.SemiBold, White, AccentBlue, new Vector2(80f, 34f));
            LayoutElement btnLe = viewBtn.gameObject.GetComponent<LayoutElement>();
            if (btnLe == null) btnLe = viewBtn.gameObject.AddComponent<LayoutElement>();
            btnLe.preferredWidth = 80f;
            btnLe.minWidth = 80f;
            btnLe.flexibleWidth = 0f;
        }

        // 手动计算并强制设置 CardContainer 高度
        // ContentSizeFitter 在编辑器里不自动刷新，必须手动设定
        int rowCount = cardContainer.childCount;
        float rowH = 60f;
        float spacing = 8f;
        float paddingV = 4f + 4f; // top + bottom
        float totalH = rowCount * rowH + Mathf.Max(0, rowCount - 1) * spacing + paddingV;
        RectTransform ccRt = cardContainer as RectTransform;
        ccRt.sizeDelta = new Vector2(0f, totalH);

        // 从根节点向下全量重建布局，确保父级 Viewport/ScrollView 大小展开后 RectMask2D 才能正确裁切
        Canvas.ForceUpdateCanvases();
        // 从 CardContainer 向上找到 LabInfoPanel 根，全量重建
        Transform root = cardContainer;
        while (root.parent != null) root = root.parent;
        LayoutRebuilder.ForceRebuildLayoutImmediate(root as RectTransform);
        // 再次从 CardContainer 重建，确保子元素排列正确
        LayoutRebuilder.ForceRebuildLayoutImmediate(ccRt);
        Debug.Log($"[Builder] 列表行生成完成, 共 {rowCount} 行, CardContainer 高度={totalH}");
    }

    private static Transform FindOverviewCardRoot(Transform start)
    {
        Transform current = start;
        while (current != null)
        {
            if (current.parent != null && current.parent.name == "CardContainer")
            {
                return current;
            }

            current = current.parent;
        }

        return null;
    }

    private static void ApplyOverviewCardStyleToExisting(GameObject card)
    {
        if (card == null)
        {
            return;
        }

        RectTransform cardRt = card.GetComponent<RectTransform>();
        if (cardRt != null)
        {
            cardRt.sizeDelta = new Vector2(382f, 138f);
        }

        Image background = card.GetComponent<Image>();
        if (background == null)
        {
            background = card.AddComponent<Image>();
        }
        ApplyDefaultSprite(background);
        background.color = OverviewCardBg;
        background.raycastTarget = true;

        Outline outline = card.GetComponent<Outline>();
        if (outline == null)
        {
            outline = card.AddComponent<Outline>();
        }
        outline.effectColor = OverviewCardBorder;
        outline.effectDistance = new Vector2(2f, -2f);

        Shadow shadow = card.GetComponent<Shadow>();
        if (shadow != null)
        {
            Object.DestroyImmediate(shadow, true);
        }

        Button button = card.GetComponent<Button>();
        if (button == null)
        {
            button = card.AddComponent<Button>();
        }
        button.targetGraphic = background;
        ColorBlock colors = button.colors;
        colors.normalColor = OverviewCardBg;
        colors.highlightedColor = Multiply(OverviewCardBg, 1.12f);
        colors.pressedColor = Multiply(OverviewCardBg, 0.88f);
        colors.selectedColor = OverviewCardBg;
        button.colors = colors;

        Image topBand = EnsureStyledImageChild(card, "TopBand", OverviewCardHeader);
        ApplyRect(topBand.rectTransform, new Vector2(0f, 1f), new Vector2(1f, 1f), new Vector2(0.5f, 1f), new Vector2(0f, 46f), Vector2.zero);
        topBand.transform.SetSiblingIndex(0);

        Image footerBand = EnsureStyledImageChild(card, "FooterBand", OverviewCardFooter);
        ApplyRect(footerBand.rectTransform, new Vector2(0f, 0f), new Vector2(1f, 0f), new Vector2(0.5f, 0f), new Vector2(0f, 34f), Vector2.zero);
        footerBand.transform.SetSiblingIndex(1);

        Image accent = EnsureStyledImageChild(card, "AccentBar", AccentBlueSoft);
        ApplyRect(accent.rectTransform, new Vector2(0f, 0f), new Vector2(0f, 1f), new Vector2(0f, 0.5f), new Vector2(8f, 0f), Vector2.zero);
        accent.transform.SetSiblingIndex(2);

        TextMeshProUGUI labName = EnsureTextChild(card, "LabNameText", "实验室名称", 19, FontWeight.SemiBold, TextPrimary, TextAlignmentOptions.Left);
        ApplyRect(labName.rectTransform, new Vector2(0f, 1f), new Vector2(0.72f, 1f), new Vector2(0f, 1f), new Vector2(0f, 30f), new Vector2(20f, -18f));

        GameObject statusBadgeGo = card.transform.Find("StatusBadge")?.gameObject;
        if (statusBadgeGo == null)
        {
            statusBadgeGo = CreateElement("StatusBadge", card);
        }
        Image statusBadge = statusBadgeGo.GetComponent<Image>();
        if (statusBadge == null)
        {
            statusBadge = statusBadgeGo.AddComponent<Image>();
        }
        ApplyDefaultSprite(statusBadge);
        statusBadge.raycastTarget = false;
        ApplyRect(statusBadge.rectTransform, new Vector2(1f, 1f), new Vector2(1f, 1f), new Vector2(1f, 1f), new Vector2(76f, 26f), new Vector2(-16f, -14f));

        TextMeshProUGUI statusText = EnsureTextChild(statusBadgeGo, "StatusText", "开放中", 11, FontWeight.SemiBold, White, TextAlignmentOptions.Center);
        StretchFull(statusText.rectTransform);

        TextMeshProUGUI location = EnsureTextChild(card, "LocationText", "地点：东校区 · 教学楼A 3F A301", 14, FontWeight.Medium, OverviewLocationText, TextAlignmentOptions.Left);
        ApplyRect(location.rectTransform, new Vector2(0f, 1f), new Vector2(1f, 1f), new Vector2(0f, 1f), new Vector2(0f, 22f), new Vector2(20f, -62f));

        TextMeshProUGUI hint = EnsureTextChild(card, "ClickHintText", "点击查看详情", 12, FontWeight.Medium, OverviewHintText, TextAlignmentOptions.Left);
        ApplyRect(hint.rectTransform, new Vector2(0f, 0f), new Vector2(0.5f, 0f), new Vector2(0f, 0f), new Vector2(0f, 20f), new Vector2(20f, 8f));

        TextMeshProUGUI chevron = EnsureTextChild(card, "ChevronText", ">", 24, FontWeight.SemiBold, OverviewHintText, TextAlignmentOptions.Center);
        ApplyRect(chevron.rectTransform, new Vector2(1f, 0f), new Vector2(1f, 0f), new Vector2(1f, 0f), new Vector2(32f, 26f), new Vector2(-16f, 8f));

        SetExistingTextStyle(card.transform.Find("LabCodeText"), 11, FontWeight.Regular, TextSecondary, false);
        SetExistingTextStyle(card.transform.Find("DescriptionText"), 12, FontWeight.Regular, TextPrimary, false);
        SetExistingTextStyle(card.transform.Find("EquipmentText"), 11, FontWeight.Regular, TextSecondary, false);

        Transform metaPanel = card.transform.Find("MetaPanel");
        if (metaPanel != null)
        {
            metaPanel.gameObject.SetActive(false);
        }

        Transform labCodeText = card.transform.Find("LabCodeText");
        if (labCodeText != null)
        {
            labCodeText.gameObject.SetActive(false);
        }

        Transform descriptionText = card.transform.Find("DescriptionText");
        if (descriptionText != null)
        {
            descriptionText.gameObject.SetActive(false);
        }

        Transform equipmentText = card.transform.Find("EquipmentText");
        if (equipmentText != null)
        {
            equipmentText.gameObject.SetActive(false);
        }
    }

    private static void ApplyCardPreviewData(GameObject card, LabInfoData lab)
    {
        SetCardText(card.transform.Find("InfoGroup/LabNameText"), lab.labName);
        SetCardText(card.transform.Find("InfoGroup/SubRow/LocationText"), lab.FullLocation);
        SetCardText(card.transform.Find("InfoGroup/SubRow/StatusText"), lab.StatusName);

        // 状态文字颜色
        var statusText = card.transform.Find("InfoGroup/SubRow/StatusText")?.GetComponent<TextMeshProUGUI>();
        if (statusText != null && ColorUtility.TryParseHtmlString(lab.StatusColor, out Color sc))
            statusText.color = sc;

        // 状态圆点颜色
        var dot = card.transform.Find("StatusBadge")?.GetComponent<Image>();
        if (dot != null && ColorUtility.TryParseHtmlString(lab.StatusColor, out Color dc))
            dot.color = dc;
    }

    private static void SetCardText(Transform target, string value)
    {
        TextMeshProUGUI text = target?.GetComponent<TextMeshProUGUI>();
        if (text != null)
        {
            text.text = value;
        }
    }

    private static TextMeshProUGUI CreateText(
        string name,
        GameObject parent,
        string text,
        float fontSize,
        FontWeight fontWeight,
        Color color,
        TextAlignmentOptions alignment)
    {
        GameObject go = CreateElement(name, parent);
        TextMeshProUGUI tmp = go.AddComponent<TextMeshProUGUI>();
        tmp.font = ResolveFont();
        tmp.text = text;
        tmp.fontSize = fontSize;
        tmp.fontWeight = fontWeight;
        tmp.color = color;
        tmp.alignment = alignment;
        tmp.enableWordWrapping = false;
        tmp.raycastTarget = false;
        return tmp;
    }

    private static TextMeshProUGUI EnsureTextChild(
        GameObject parent,
        string name,
        string text,
        float fontSize,
        FontWeight fontWeight,
        Color color,
        TextAlignmentOptions alignment)
    {
        Transform existing = parent.transform.Find(name);
        TextMeshProUGUI tmp = existing != null
            ? existing.GetComponent<TextMeshProUGUI>()
            : null;

        if (tmp == null)
        {
            tmp = CreateText(name, parent, text, fontSize, fontWeight, color, alignment);
        }
        else
        {
            tmp.font = ResolveFont();
            tmp.text = string.IsNullOrEmpty(tmp.text) ? text : tmp.text;
            tmp.fontSize = fontSize;
            tmp.fontWeight = fontWeight;
            tmp.color = color;
            tmp.alignment = alignment;
            tmp.enableWordWrapping = false;
            tmp.raycastTarget = false;
            tmp.gameObject.SetActive(true);
        }

        return tmp;
    }

    private static void SetExistingTextStyle(Transform target, float fontSize, FontWeight fontWeight, Color color, bool visible)
    {
        TextMeshProUGUI text = target?.GetComponent<TextMeshProUGUI>();
        if (text == null)
        {
            return;
        }

        text.font = ResolveFont();
        text.fontSize = fontSize;
        text.fontWeight = fontWeight;
        text.color = color;
        text.raycastTarget = false;
        text.gameObject.SetActive(visible);
    }

    private static Image EnsureStyledImageChild(GameObject parent, string name, Color color)
    {
        Transform existing = parent.transform.Find(name);
        GameObject go = existing != null ? existing.gameObject : CreateElement(name, parent);
        Image image = go.GetComponent<Image>();
        if (image == null)
        {
            image = go.AddComponent<Image>();
        }

        ApplyDefaultSprite(image);
        image.color = color;
        image.raycastTarget = false;
        go.SetActive(true);
        return image;
    }

    private static void ApplyRect(RectTransform rt, Vector2 anchorMin, Vector2 anchorMax, Vector2 pivot, Vector2 sizeDelta, Vector2 anchoredPosition)
    {
        rt.anchorMin = anchorMin;
        rt.anchorMax = anchorMax;
        rt.pivot = pivot;
        rt.sizeDelta = sizeDelta;
        rt.anchoredPosition = anchoredPosition;
    }

    private const string WhitePixelPath = "Assets/Prefabs/UI/WhitePixel.png";

    private static Sprite ResolveUiSprite()
    {
        if (cachedUiSprite != null)
        {
            return cachedUiSprite;
        }

        // 优先使用项目内持久化的 WhitePixel 资产（可序列化，不会丢失引用）
        cachedUiSprite = AssetDatabase.LoadAssetAtPath<Sprite>(WhitePixelPath);
        if (cachedUiSprite != null)
        {
            return cachedUiSprite;
        }

        // 如果不存在则生成并保存到磁盘
        Texture2D tex = new Texture2D(4, 4, TextureFormat.RGBA32, false);
        Color[] pixels = new Color[16];
        for (int i = 0; i < 16; i++) pixels[i] = Color.white;
        tex.SetPixels(pixels);
        tex.Apply();
        byte[] pngData = tex.EncodeToPNG();
        Object.DestroyImmediate(tex);

        string dir = System.IO.Path.GetDirectoryName(WhitePixelPath);
        if (!System.IO.Directory.Exists(dir))
            System.IO.Directory.CreateDirectory(dir);
        System.IO.File.WriteAllBytes(WhitePixelPath, pngData);
        AssetDatabase.ImportAsset(WhitePixelPath);

        // 设置导入选项：Sprite 类型
        var importer = AssetImporter.GetAtPath(WhitePixelPath) as UnityEditor.TextureImporter;
        if (importer != null)
        {
            importer.textureType = UnityEditor.TextureImporterType.Sprite;
            importer.spriteImportMode = UnityEditor.SpriteImportMode.Single;
            importer.SaveAndReimport();
        }

        cachedUiSprite = AssetDatabase.LoadAssetAtPath<Sprite>(WhitePixelPath);
        if (cachedUiSprite == null)
        {
            // 最终兜底：内置精灵
            cachedUiSprite = AssetDatabase.GetBuiltinExtraResource<Sprite>("UI/Skin/Background.psd");
        }

        return cachedUiSprite;
    }

    private static void ApplyDefaultSprite(Image image)
    {
        if (image == null)
        {
            return;
        }

        image.sprite = ResolveUiSprite();
        image.type = Image.Type.Simple;
        image.preserveAspect = false;
    }

    private static Button CreateButton(
        string name,
        GameObject parent,
        string label,
        float fontSize,
        FontWeight fontWeight,
        Color textColor,
        Color backgroundColor,
        Vector2 size)
    {
        GameObject buttonGo = CreateElement(name, parent);
        RectTransform buttonRt = buttonGo.GetComponent<RectTransform>();
        buttonRt.sizeDelta = size;

        Image image = buttonGo.AddComponent<Image>();
        image.color = backgroundColor;

        Button button = buttonGo.AddComponent<Button>();
        button.targetGraphic = image;

        ColorBlock colors = button.colors;
        colors.normalColor = backgroundColor;
        colors.highlightedColor = Multiply(backgroundColor, 1.08f);
        colors.pressedColor = Multiply(backgroundColor, 0.9f);
        colors.selectedColor = backgroundColor;
        button.colors = colors;

        TextMeshProUGUI labelText = CreateText(
            "Label",
            buttonGo,
            label,
            fontSize,
            fontWeight,
            textColor,
            TextAlignmentOptions.Center);
        StretchFull(labelText.GetComponent<RectTransform>());

        return button;
    }

    private static GameObject CreatePanelCard(string name, GameObject parent, Color backgroundColor, Color borderColor)
    {
        GameObject go = CreateElement(name, parent);
        Image image = go.AddComponent<Image>();
        ApplyDefaultSprite(image);
        image.color = backgroundColor;

        Outline outline = go.AddComponent<Outline>();
        outline.effectColor = borderColor;
        outline.effectDistance = new Vector2(1f, 1f);

        return go;
    }

    private static void CreateSeparator(string name, GameObject parent, float anchoredY)
    {
        GameObject separator = CreateElement(name, parent);
        RectTransform separatorRt = separator.GetComponent<RectTransform>();
        separatorRt.anchorMin = new Vector2(0f, 1f);
        separatorRt.anchorMax = new Vector2(1f, 1f);
        separatorRt.pivot = new Vector2(0.5f, 1f);
        separatorRt.sizeDelta = new Vector2(0f, 1f);
        separatorRt.anchoredPosition = new Vector2(0f, anchoredY);
        separator.AddComponent<Image>().color = SeparatorLine;
    }

    private static void CreateTabHighlight(string name, GameObject tab, bool active)
    {
        GameObject highlight = CreateElement(name, tab);
        RectTransform highlightRt = highlight.GetComponent<RectTransform>();
        highlightRt.anchorMin = new Vector2(0f, 0f);
        highlightRt.anchorMax = new Vector2(1f, 0f);
        highlightRt.pivot = new Vector2(0.5f, 0f);
        highlightRt.sizeDelta = new Vector2(0f, 3f);
        highlightRt.anchoredPosition = Vector2.zero;
        highlight.AddComponent<Image>().color = AccentBlue;
        highlight.SetActive(active);
    }

    private static void CreateInfoChip(GameObject parent, string text, Color color, Vector2 anchoredPosition)
    {
        GameObject chip = CreatePanelCard($"Chip_{text}", parent, Hex("#1A2333"), InputBorder);
        RectTransform chipRt = chip.GetComponent<RectTransform>();
        chipRt.anchorMin = new Vector2(0f, 1f);
        chipRt.anchorMax = new Vector2(0f, 1f);
        chipRt.pivot = new Vector2(0f, 1f);
        chipRt.sizeDelta = new Vector2(104f, 30f);
        chipRt.anchoredPosition = anchoredPosition;

        GameObject accent = CreateElement("Accent", chip);
        RectTransform accentRt = accent.GetComponent<RectTransform>();
        accentRt.anchorMin = new Vector2(0f, 0.5f);
        accentRt.anchorMax = new Vector2(0f, 0.5f);
        accentRt.pivot = new Vector2(0f, 0.5f);
        accentRt.sizeDelta = new Vector2(6f, 14f);
        accentRt.anchoredPosition = new Vector2(12f, 0f);
        accent.AddComponent<Image>().color = color;

        TextMeshProUGUI chipText = CreateText(
            "ChipText",
            chip,
            text,
            11,
            FontWeight.Medium,
            TextSecondary,
            TextAlignmentOptions.Left);
        RectTransform chipTextRt = chipText.GetComponent<RectTransform>();
        chipTextRt.anchorMin = new Vector2(0f, 0f);
        chipTextRt.anchorMax = new Vector2(1f, 1f);
        chipTextRt.offsetMin = new Vector2(26f, 0f);
        chipTextRt.offsetMax = new Vector2(-10f, 0f);
    }

    private static void CreateBullet(GameObject parent, string text, Vector2 anchoredPosition, int lines = 1)
    {
        GameObject bullet = CreateElement($"Bullet_{Mathf.Abs(anchoredPosition.y)}", parent);
        RectTransform bulletRt = bullet.GetComponent<RectTransform>();
        bulletRt.anchorMin = new Vector2(0f, 1f);
        bulletRt.anchorMax = new Vector2(1f, 1f);
        bulletRt.pivot = new Vector2(0f, 1f);
        bulletRt.sizeDelta = new Vector2(0f, 24f * lines);
        bulletRt.anchoredPosition = anchoredPosition;

        GameObject dot = CreateElement("Dot", bullet);
        RectTransform dotRt = dot.GetComponent<RectTransform>();
        dotRt.anchorMin = new Vector2(0f, 1f);
        dotRt.anchorMax = new Vector2(0f, 1f);
        dotRt.pivot = new Vector2(0f, 1f);
        dotRt.sizeDelta = new Vector2(8f, 8f);
        dotRt.anchoredPosition = new Vector2(0f, -6f);
        dot.AddComponent<Image>().color = AccentBlue;

        TextMeshProUGUI bulletText = CreateText(
            "BulletText",
            bullet,
            text,
            12,
            FontWeight.Regular,
            TextSecondary,
            TextAlignmentOptions.TopLeft);
        RectTransform bulletTextRt = bulletText.GetComponent<RectTransform>();
        bulletTextRt.anchorMin = new Vector2(0f, 0f);
        bulletTextRt.anchorMax = new Vector2(1f, 1f);
        bulletTextRt.offsetMin = new Vector2(18f, 0f);
        bulletTextRt.offsetMax = new Vector2(-16f, 0f);
        bulletText.enableWordWrapping = true;
    }

    private static GameObject CreateFieldRow(GameObject parent, string name)
    {
        GameObject row = CreateElement(name, parent);
        RectTransform rowRt = row.GetComponent<RectTransform>();
        rowRt.sizeDelta = new Vector2(0f, 74f);

        LayoutElement rowLayoutElement = row.AddComponent<LayoutElement>();
        rowLayoutElement.preferredHeight = 74f;
        rowLayoutElement.flexibleWidth = 1f;

        HorizontalLayoutGroup rowLayout = row.AddComponent<HorizontalLayoutGroup>();
        rowLayout.spacing = 14f;
        rowLayout.childForceExpandWidth = true;
        rowLayout.childForceExpandHeight = false;
        rowLayout.childControlWidth = true;
        rowLayout.childControlHeight = false;

        return row;
    }

    private static GameObject CreateInputField(GameObject parent, string name, string placeholder, float height = 74f, bool multiLine = false)
    {
        GameObject wrapper = CreateElement(name + "Wrapper", parent);
        RectTransform wrapperRt = wrapper.GetComponent<RectTransform>();
        wrapperRt.sizeDelta = new Vector2(0f, height);

        LayoutElement wrapperLayoutElement = wrapper.AddComponent<LayoutElement>();
        wrapperLayoutElement.preferredHeight = height;
        wrapperLayoutElement.flexibleWidth = 1f;

        VerticalLayoutGroup wrapperLayout = wrapper.AddComponent<VerticalLayoutGroup>();
        wrapperLayout.spacing = 6f;
        wrapperLayout.childForceExpandWidth = true;
        wrapperLayout.childForceExpandHeight = false;
        wrapperLayout.childControlWidth = true;
        wrapperLayout.childControlHeight = false;

        TextMeshProUGUI label = CreateText(
            "FieldLabel",
            wrapper,
            placeholder,
            12,
            FontWeight.Medium,
            TextSecondary,
            TextAlignmentOptions.Left);
        RectTransform labelRt = label.GetComponent<RectTransform>();
        labelRt.sizeDelta = new Vector2(0f, 20f);

        GameObject inputGo = CreatePanelCard(name, wrapper, InputBg, InputBorder);
        RectTransform inputRt = inputGo.GetComponent<RectTransform>();
        inputRt.sizeDelta = new Vector2(0f, multiLine ? 86f : 44f);

        TMP_InputField input = inputGo.AddComponent<TMP_InputField>();

        GameObject textArea = CreateElement("Text Area", inputGo);
        RectTransform textAreaRt = textArea.GetComponent<RectTransform>();
        textAreaRt.anchorMin = Vector2.zero;
        textAreaRt.anchorMax = Vector2.one;
        textAreaRt.offsetMin = new Vector2(14f, 8f);
        textAreaRt.offsetMax = new Vector2(-14f, -8f);
        textArea.AddComponent<RectMask2D>();

        TextMeshProUGUI placeholderText = CreateText(
            "Placeholder",
            textArea,
            multiLine ? "请填写借用目的、课程或活动说明..." : placeholder,
            13,
            FontWeight.Regular,
            TextMuted,
            TextAlignmentOptions.TopLeft);
        StretchFull(placeholderText.GetComponent<RectTransform>());
        placeholderText.enableWordWrapping = multiLine;

        TextMeshProUGUI inputText = CreateText(
            "Text",
            textArea,
            string.Empty,
            13,
            FontWeight.Regular,
            TextPrimary,
            TextAlignmentOptions.TopLeft);
        StretchFull(inputText.GetComponent<RectTransform>());
        inputText.enableWordWrapping = multiLine;

        input.textViewport = textAreaRt;
        input.textComponent = inputText;
        input.placeholder = placeholderText;
        input.lineType = multiLine ? TMP_InputField.LineType.MultiLineNewline : TMP_InputField.LineType.SingleLine;

        return inputGo;
    }

    private static GameObject CreateDropdownField(GameObject parent, string name, string labelText)
    {
        GameObject wrapper = CreateElement(name + "Wrapper", parent);
        RectTransform wrapperRt = wrapper.GetComponent<RectTransform>();
        wrapperRt.sizeDelta = new Vector2(0f, 74f);

        LayoutElement wrapperLayoutElement = wrapper.AddComponent<LayoutElement>();
        wrapperLayoutElement.preferredHeight = 74f;
        wrapperLayoutElement.flexibleWidth = 1f;

        VerticalLayoutGroup wrapperLayout = wrapper.AddComponent<VerticalLayoutGroup>();
        wrapperLayout.spacing = 6f;
        wrapperLayout.childForceExpandWidth = true;
        wrapperLayout.childForceExpandHeight = false;
        wrapperLayout.childControlWidth = true;
        wrapperLayout.childControlHeight = false;

        TextMeshProUGUI label = CreateText(
            "FieldLabel",
            wrapper,
            labelText,
            12,
            FontWeight.Medium,
            TextSecondary,
            TextAlignmentOptions.Left);
        RectTransform labelRt = label.GetComponent<RectTransform>();
        labelRt.sizeDelta = new Vector2(0f, 20f);

        GameObject dropdownGo = CreatePanelCard(name, wrapper, InputBg, InputBorder);
        RectTransform dropdownRt = dropdownGo.GetComponent<RectTransform>();
        dropdownRt.sizeDelta = new Vector2(0f, 44f);

        TMP_Dropdown dropdown = dropdownGo.AddComponent<TMP_Dropdown>();
        dropdown.targetGraphic = dropdownGo.GetComponent<Image>();

        TextMeshProUGUI caption = CreateText(
            "Label",
            dropdownGo,
            labelText,
            13,
            FontWeight.Regular,
            TextPrimary,
            TextAlignmentOptions.Left);
        RectTransform captionRt = caption.GetComponent<RectTransform>();
        captionRt.anchorMin = new Vector2(0f, 0f);
        captionRt.anchorMax = new Vector2(0.86f, 1f);
        captionRt.offsetMin = new Vector2(14f, 0f);
        captionRt.offsetMax = new Vector2(0f, 0f);
        dropdown.captionText = caption;

        TextMeshProUGUI arrow = CreateText(
            "Arrow",
            dropdownGo,
            "▼",
            10,
            FontWeight.Medium,
            TextSecondary,
            TextAlignmentOptions.Center);
        RectTransform arrowRt = arrow.GetComponent<RectTransform>();
        arrowRt.anchorMin = new Vector2(0.86f, 0.22f);
        arrowRt.anchorMax = new Vector2(1f, 0.78f);
        arrowRt.offsetMin = Vector2.zero;
        arrowRt.offsetMax = new Vector2(-10f, 0f);

        GameObject template = CreatePanelCard("Template", dropdownGo, CardBg, CardBorder);
        RectTransform templateRt = template.GetComponent<RectTransform>();
        templateRt.anchorMin = new Vector2(0f, 0f);
        templateRt.anchorMax = new Vector2(1f, 0f);
        templateRt.pivot = new Vector2(0.5f, 1f);
        templateRt.sizeDelta = new Vector2(0f, 240f);
        templateRt.anchoredPosition = new Vector2(0f, -4f);

        ScrollRect templateScroll = template.AddComponent<ScrollRect>();
        templateScroll.horizontal = false;
        templateScroll.vertical = true;
        templateScroll.movementType = ScrollRect.MovementType.Clamped;

        GameObject viewport = CreateElement("Viewport", template);
        StretchFull(viewport.GetComponent<RectTransform>());
        Image viewportImg = viewport.AddComponent<Image>();
        viewportImg.color = new Color(0f, 0f, 0f, 0f);
        viewport.AddComponent<Mask>().showMaskGraphic = false;
        templateScroll.viewport = viewport.GetComponent<RectTransform>();

        GameObject content = CreateElement("Content", viewport);
        RectTransform contentRt = content.GetComponent<RectTransform>();
        contentRt.anchorMin = new Vector2(0f, 1f);
        contentRt.anchorMax = new Vector2(1f, 1f);
        contentRt.pivot = new Vector2(0.5f, 1f);
        contentRt.anchoredPosition = Vector2.zero;
        contentRt.sizeDelta = new Vector2(0f, 30f);
        templateScroll.content = contentRt;
        VerticalLayoutGroup contentLayout = content.AddComponent<VerticalLayoutGroup>();
        contentLayout.spacing = 0f;
        contentLayout.childAlignment = TextAnchor.UpperLeft;
        contentLayout.childControlWidth = true;
        contentLayout.childControlHeight = true;
        contentLayout.childForceExpandWidth = true;
        contentLayout.childForceExpandHeight = false;
        ContentSizeFitter contentFitter = content.AddComponent<ContentSizeFitter>();
        contentFitter.horizontalFit = ContentSizeFitter.FitMode.Unconstrained;
        contentFitter.verticalFit = ContentSizeFitter.FitMode.PreferredSize;

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

        GameObject itemBackground = CreateElement("Item Background", item);
        StretchFull(itemBackground.GetComponent<RectTransform>());
        Image itemBackgroundImage = itemBackground.AddComponent<Image>();
        itemBackgroundImage.color = new Color(0f, 0f, 0f, 0f);
        itemToggle.targetGraphic = itemBackgroundImage;

        GameObject itemCheckmark = CreateElement("Item Checkmark", item);
        RectTransform itemCheckmarkRt = itemCheckmark.GetComponent<RectTransform>();
        itemCheckmarkRt.anchorMin = new Vector2(0f, 0f);
        itemCheckmarkRt.anchorMax = new Vector2(0f, 1f);
        itemCheckmarkRt.pivot = new Vector2(0f, 0.5f);
        itemCheckmarkRt.sizeDelta = new Vector2(14f, 0f);
        itemCheckmarkRt.anchoredPosition = new Vector2(10f, 0f);
        Image itemCheckmarkImage = itemCheckmark.AddComponent<Image>();
        itemCheckmarkImage.color = AccentBlue;
        itemToggle.graphic = itemCheckmarkImage;

        TextMeshProUGUI itemLabel = CreateText(
            "Item Label",
            item,
            labelText,
            13,
            FontWeight.Regular,
            TextPrimary,
            TextAlignmentOptions.Left);
        RectTransform itemLabelRt = itemLabel.GetComponent<RectTransform>();
        itemLabelRt.anchorMin = new Vector2(0f, 0f);
        itemLabelRt.anchorMax = new Vector2(1f, 1f);
        itemLabelRt.offsetMin = new Vector2(28f, 0f);
        itemLabelRt.offsetMax = new Vector2(-10f, 0f);

        dropdown.template = templateRt;
        dropdown.itemText = itemLabel;
        dropdown.itemImage = itemCheckmarkImage;
        template.SetActive(false);

        return dropdownGo;
    }

    private static GameObject CreateElement(string name, GameObject parent)
    {
        GameObject go = new GameObject(name);
        go.AddComponent<RectTransform>();
        if (parent != null)
        {
            go.transform.SetParent(parent.transform, false);
        }

        return go;
    }

    private static void StretchFull(RectTransform rt)
    {
        rt.anchorMin = Vector2.zero;
        rt.anchorMax = Vector2.one;
        rt.offsetMin = Vector2.zero;
        rt.offsetMax = Vector2.zero;
    }

    private static void AutoBindReferences(GameObject root)
    {
        LabInfoPanelUI ui = root.GetComponent<LabInfoPanelUI>();
        if (ui == null)
        {
            return;
        }

        Transform FindAt(string path) => root.transform.Find(path);
        const string basePath = "OverlayBg/MainPanel";

        ui.titleText = FindAt($"{basePath}/HeaderBar/TitleText")?.GetComponent<TextMeshProUGUI>();
        ui.closeButton = FindAt($"{basePath}/HeaderBar/CloseButton")?.GetComponent<Button>();
        ui.tabOverviewButton = FindAt($"{basePath}/TabBar/TabOverview")?.GetComponent<Button>();
        ui.tabBorrowButton = FindAt($"{basePath}/TabBar/TabBorrow")?.GetComponent<Button>();
        ui.tabOverviewHighlight = FindAt($"{basePath}/TabBar/TabOverview/TabOverviewHighlight")?.gameObject;
        ui.tabBorrowHighlight = FindAt($"{basePath}/TabBar/TabBorrow/TabBorrowHighlight")?.gameObject;
        ui.overviewPanel = FindAt($"{basePath}/OverviewPanel")?.gameObject;
        ui.cardContainer = FindAt($"{basePath}/OverviewPanel/ScrollView/Viewport/CardContainer");
        ui.cardPrefab = FindAt("TemplateRoot/CardTemplate")?.gameObject;
        ui.overviewSummaryText = FindAt($"{basePath}/OverviewPanel/SummaryCard/OverviewSummaryText")?.GetComponent<TextMeshProUGUI>();
        ui.detailOverlay = FindAt($"{basePath}/OverviewPanel/DetailOverlay")?.gameObject;
        ui.detailOverlayButton = FindAt($"{basePath}/OverviewPanel/DetailOverlay")?.GetComponent<Button>();
        ui.detailCloseButton = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/DetailCloseButton")?.GetComponent<Button>();
        ui.detailBorrowButton = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/DetailBorrowButton")?.GetComponent<Button>();
        ui.detailLabNameText = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/DetailLabNameText")?.GetComponent<TextMeshProUGUI>();
        ui.detailLabCodeText = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/DetailLabCodeText")?.GetComponent<TextMeshProUGUI>();
        ui.detailStatusBadge = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/DetailStatusBadge")?.GetComponent<Image>();
        ui.detailStatusText = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/DetailStatusBadge/DetailStatusText")?.GetComponent<TextMeshProUGUI>();
        ui.detailLocationText = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/DetailLocationText")?.GetComponent<TextMeshProUGUI>();
        ui.detailDescriptionText = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/DetailDescriptionText")?.GetComponent<TextMeshProUGUI>();
        ui.detailCapacityText = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/CapacityMetricCard/DetailCapacityText")?.GetComponent<TextMeshProUGUI>();
        ui.detailAreaText = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/AreaMetricCard/DetailAreaText")?.GetComponent<TextMeshProUGUI>();
        ui.detailDutyPersonText = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/DutyMetricCard/DetailDutyPersonText")?.GetComponent<TextMeshProUGUI>();
        ui.detailEquipmentText = FindAt($"{basePath}/OverviewPanel/DetailOverlay/DetailModal/DetailEquipmentText")?.GetComponent<TextMeshProUGUI>();
        ui.borrowPanel = FindAt($"{basePath}/BorrowPanel")?.gameObject;
        ui.borrowFormController = FindAt($"{basePath}/BorrowPanel")?.GetComponent<LabBorrowFormUI>();
        ui.loadingOverlay = FindAt("OverlayBg/LoadingOverlay")?.gameObject;
        ui.loadingText = FindAt("OverlayBg/LoadingOverlay/LoadingText")?.GetComponent<TextMeshProUGUI>();
        ui.tabActiveColor = TabActive;
        ui.tabInactiveColor = TabInactive;

        // 直接绑定场景中的 LabDataProvider，避免运行时查找失败
        LabDataProvider[] providers = Resources.FindObjectsOfTypeAll<LabDataProvider>();
        foreach (var p in providers)
        {
            if (p != null && p.gameObject.scene.IsValid())
            {
                ui.dataProvider = p;
                break;
            }
        }

        EditorUtility.SetDirty(ui);
        EditorUtility.SetDirty(root);
    }

    private static void AutoConnectToScreenTrigger(GameObject root)
    {
        ScreenTrigger trigger = Object.FindObjectOfType<ScreenTrigger>();
        if (trigger != null)
        {
            trigger.labInfoPanel = root;
            EditorUtility.SetDirty(trigger);
        }
    }

    private static Color Hex(string hex)
    {
        ColorUtility.TryParseHtmlString(hex, out Color color);
        return color;
    }

    private static Color Multiply(Color color, float factor)
    {
        return new Color(
            Mathf.Clamp01(color.r * factor),
            Mathf.Clamp01(color.g * factor),
            Mathf.Clamp01(color.b * factor),
            color.a);
    }
}
