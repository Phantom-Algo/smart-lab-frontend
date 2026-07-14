using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// 大屏信息面板主控制器
/// 包含两个 Tab：
///   Tab 0 — 实验室概览（卡片列表展示所有同类型实验室）
///   Tab 1 — 借用申请（预约表单）
/// </summary>
public class LabInfoPanelUI : MonoBehaviour
{
    [Header("=== 顶部标题栏 ===")]
    public TextMeshProUGUI titleText;
    public Button closeButton;

    [Header("=== Tab 切换 ===")]
    public Button tabOverviewButton;
    public Button tabBorrowButton;
    public GameObject tabOverviewHighlight;
    public GameObject tabBorrowHighlight;
    public Color tabActiveColor = new Color(0.09f, 0.13f, 0.19f);
    public Color tabInactiveColor = new Color(0.14f, 0.19f, 0.28f);

    [Header("=== Tab 0: 实验室概览 ===")]
    public GameObject overviewPanel;
    public Transform cardContainer;
    public GameObject cardPrefab;
    public TextMeshProUGUI overviewSummaryText;

    [Header("=== 数据源 ===")]
    [Tooltip("由 Builder 自动绑定，无需手动设置")]
    public LabDataProvider dataProvider;
    [Tooltip("覆盖 LabDataProvider.currentSceneLabType，由 Builder 写入，留空则读 provider 值")]
    public string sceneLabTypeOverride = "";
    public GameObject detailOverlay;
    public Button detailOverlayButton;
    public Button detailCloseButton;
    public Button detailBorrowButton;
    public TextMeshProUGUI detailLabNameText;
    public TextMeshProUGUI detailLabCodeText;
    public Image detailStatusBadge;
    public TextMeshProUGUI detailStatusText;
    public TextMeshProUGUI detailLocationText;
    public TextMeshProUGUI detailDescriptionText;
    public TextMeshProUGUI detailCapacityText;
    public TextMeshProUGUI detailAreaText;
    public TextMeshProUGUI detailDutyPersonText;
    public TextMeshProUGUI detailEquipmentText;

    [Header("=== Tab 1: 借用申请 ===")]
    public GameObject borrowPanel;
    public LabBorrowFormUI borrowFormController;

    [Header("=== 状态提示 ===")]
    public GameObject loadingOverlay;
    public TextMeshProUGUI loadingText;

    [Header("=== 调试 ===")]
    [Tooltip("勾选后在 Console 输出详细调试日志")]
    public bool debugMode = true;

    private int currentTab = 0;
    private bool dataRefreshed = false;
    private List<LabInfoData> currentLabs = new List<LabInfoData>();
    private LabInfoData currentDetailLab;
    private TMP_FontAsset runtimeFont;

    private void Awake()
    {
        if (Application.isPlaying)
        {
            gameObject.SetActive(false);
        }
    }

    private void Start()
    {
        BindButtonEvents();
    }

    private void OnEnable()
    {
        Debug.Log($"[LabInfoPanelUI] OnEnable — gameObject.activeSelf={gameObject.activeSelf}, activeInHierarchy={gameObject.activeInHierarchy}, dataRefreshed={dataRefreshed}");
        BindButtonEvents();
        if (!dataRefreshed)
        {
            RefreshData();
            dataRefreshed = true;
        }
    }

    private void OnDisable()
    {
        dataRefreshed = false;
    }

    public void RefreshData()
    {
        Debug.Log($"[LabInfoPanelUI] RefreshData 开始 — overviewPanel={overviewPanel != null}, cardContainer={cardContainer != null}, borrowPanel={borrowPanel != null}");

        // 优先使用 Builder 在编辑器阶段序列化好的直接引用
        LabDataProvider provider = dataProvider;
        if (provider == null) provider = LabDataProvider.Instance;
        if (provider == null)
        {
            LabDataProvider[] all = Resources.FindObjectsOfTypeAll<LabDataProvider>();
            foreach (var p in all)
            {
                if (p != null && p.gameObject != null)
                {
                    provider = p;
                    break;
                }
            }
        }

        List<LabInfoData> labs;
        string labType = !string.IsNullOrEmpty(sceneLabTypeOverride) ? sceneLabTypeOverride : "教学实验室";

        if (provider != null)
        {
            if (provider.mockLabData == null || provider.mockLabData.Count == 0)
                provider.GenerateMockDataPublic();

            List<LabInfoData> allLabs = provider.GetAllLabs();
            List<string> dbTypes = ResolveDbLabTypes(labType);
            labs = allLabs.FindAll(l => dbTypes.Contains(l.labTypeName));
            // 不再 fallback 到全部 — 每个场景只展示自己对应的实验室类型
            if (labs.Count == 0)
                Debug.Log($"[LabInfoPanelUI] 场景 '{labType}' 类型 [{string.Join(",", dbTypes)}] 无数据");
        }
        else
        {
            labs = null;
        }

        if (labs == null || labs.Count == 0)
        {
            Debug.LogWarning("[LabInfoPanelUI] 无实验室数据可用");
            labs = new List<LabInfoData>();
        }

        currentLabs = labs;
        Debug.Log($"[LabInfoPanelUI] 场景 '{labType}' → {labs.Count} 间实验室");

        if (titleText != null)
            titleText.text = "实验室 — 信息展示大屏";

        // 更新统计摘要
        int total = labs.Count;
        int openCount = labs.FindAll(l => l.runningStatus == 1).Count;
        int maintainCount = labs.FindAll(l => l.runningStatus == 2).Count;
        int closedCount = labs.FindAll(l => l.runningStatus == 3).Count;

        if (overviewSummaryText != null)
            overviewSummaryText.text = $"共 <color=#4A90D9>{total}</color> 间 · 开放中 <color=#27AE60>{openCount}</color> · 维护中 <color=#F39C12>{maintainCount}</color> · 已关闭 <color=#E74C3C>{closedCount}</color>";

        // 生成卡片（Builder 已在编辑器预设好行，此处仅更新内容）
        UpdateExistingCards(labs);

        // 刷新借用表单（运行时兜底：如果 borrowFormController 为空，尝试动态查找）
        if (borrowFormController == null)
        {
            borrowFormController = GetComponentInChildren<LabBorrowFormUI>(true);
            if (borrowFormController == null && borrowPanel != null)
                borrowFormController = borrowPanel.GetComponent<LabBorrowFormUI>();
            Debug.Log($"[LabInfoPanelUI] 运行时兜底查找 borrowFormController: {(borrowFormController != null ? "已找到" : "未找到")}");
        }

        if (borrowFormController != null)
        {
            borrowFormController.RefreshLabList(labs);
            Debug.Log($"[LabInfoPanelUI] RefreshLabList 已调用, labs 数量={labs.Count}");
        }
        else
        {
            Debug.LogError("[LabInfoPanelUI] borrowFormController 为空，无法刷新实验室下拉列表！");
        }

        HideDetailOverlay();

        // 默认切到概览 Tab
        SwitchTab(0);
    }

    /// <summary>
    /// 场景类型 → 数据库实验室类型映射：
    ///   Class_Lab="教学实验室"→教学实验室; Common_Lab="通用实验室"→化学/物理/生物;
    ///   Computer_Lab="计算机实验室"→计算机; Discussion_Lab="小组讨论室"→小组讨论室
    /// </summary>
    private static List<string> ResolveDbLabTypes(string sceneLabType)
    {
        switch (sceneLabType)
        {
            case "通用实验室":  // Common_Lab → 化学 + 物理 + 生物
                return new List<string> { "化学实验室", "物理实验室", "生物实验室" };
            case "计算机实验室": // Computer_Lab → 计算机
                return new List<string> { "计算机实验室" };
            case "小组讨论室":   // Discussion_Lab → 小组讨论室（数据库暂无，用 mock 兜底）
                return new List<string> { "小组讨论室" };
            case "教学实验室":   // Class_Lab → 教学实验室（数据库暂无此类型，用 mock 兜底）
                return new List<string> { "教学实验室" };
            default:
                return new List<string> { "教学实验室" };
        }
    }

    /// <summary>
    private void UpdateExistingCards(List<LabInfoData> labs)
    {
        if (cardContainer == null)
        {
            Debug.LogError("[LabInfoPanelUI] cardContainer 未绑定！");
            return;
        }

        Debug.Log($"[LabInfoPanelUI] UpdateExistingCards: cardContainer 子对象数={cardContainer.childCount}, labs={labs.Count}");

        EnsureCardRows(labs.Count);

        // 逐行更新已有内容
        int childCount = cardContainer.childCount;
        for (int i = 0; i < labs.Count; i++)
        {
            if (i < childCount)
            {
                // 更新现有行
                Transform row = cardContainer.GetChild(i);
                if (!IsUsableCardRow(row.gameObject))
                {
                    row.gameObject.SetActive(false);
                    row = CreateRuntimeCardRow(cardContainer).transform;
                    row.SetSiblingIndex(i);
                }

                SetupCard(row.gameObject, labs[i]);
                if (!row.gameObject.activeSelf) row.gameObject.SetActive(true);
            }
        }

        // 隐藏多余的旧行（如果 labs 数量变少）
        for (int i = labs.Count; i < childCount; i++)
        {
            cardContainer.GetChild(i).gameObject.SetActive(false);
        }

        Canvas.ForceUpdateCanvases();
        if (cardContainer is RectTransform ccRt)
            LayoutRebuilder.ForceRebuildLayoutImmediate(ccRt);

        Debug.Log($"[LabInfoPanelUI] 卡片更新完成, 可见行数={Mathf.Min(labs.Count, childCount)}");
    }

    private bool IsUsableCardRow(GameObject row)
    {
        if (row == null)
            return false;

        return row.transform.Find("RowContent/InfoGroup/LabNameText") != null
            && row.transform.Find("RowContent/InfoGroup/SubRow/LocationText") != null
            && row.transform.Find("RowContent/ViewDetailButton") != null;
    }

    private void EnsureCardRows(int requiredCount)
    {
        if (cardContainer == null)
            return;

        while (cardContainer.childCount < requiredCount)
        {
            GameObject row = CreateRuntimeCardRow(cardContainer);
            row.name = $"LabListRow_Runtime_{cardContainer.childCount:00}";
        }
    }

    private GameObject CreateRuntimeCardRow(Transform parent)
    {
        GameObject row = new GameObject("LabListRow_Runtime", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(LayoutElement));
        row.transform.SetParent(parent, false);

        RectTransform rowRt = row.GetComponent<RectTransform>();
        rowRt.sizeDelta = new Vector2(0f, 64f);
        Image rowImage = row.GetComponent<Image>();
        rowImage.color = new Color(0.08f, 0.12f, 0.18f, 0.96f);
        rowImage.raycastTarget = true;

        LayoutElement rowLayout = row.GetComponent<LayoutElement>();
        rowLayout.minHeight = 64f;
        rowLayout.preferredHeight = 64f;
        rowLayout.flexibleWidth = 1f;

        GameObject accent = new GameObject("AccentBar", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image));
        accent.transform.SetParent(row.transform, false);
        RectTransform accentRt = accent.GetComponent<RectTransform>();
        accentRt.anchorMin = new Vector2(0f, 0f);
        accentRt.anchorMax = new Vector2(0f, 1f);
        accentRt.pivot = new Vector2(0f, 0.5f);
        accentRt.sizeDelta = new Vector2(4f, 0f);
        accent.GetComponent<Image>().color = new Color(0.15f, 0.68f, 0.38f);

        GameObject content = new GameObject("RowContent", typeof(RectTransform), typeof(HorizontalLayoutGroup));
        content.transform.SetParent(row.transform, false);
        RectTransform contentRt = content.GetComponent<RectTransform>();
        contentRt.anchorMin = Vector2.zero;
        contentRt.anchorMax = Vector2.one;
        contentRt.offsetMin = new Vector2(16f, 8f);
        contentRt.offsetMax = new Vector2(-14f, -8f);
        HorizontalLayoutGroup contentLayout = content.GetComponent<HorizontalLayoutGroup>();
        contentLayout.spacing = 12f;
        contentLayout.childAlignment = TextAnchor.MiddleLeft;
        contentLayout.childControlWidth = true;
        contentLayout.childControlHeight = true;
        contentLayout.childForceExpandWidth = false;
        contentLayout.childForceExpandHeight = true;

        GameObject dot = new GameObject("StatusBadge", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(LayoutElement));
        dot.transform.SetParent(content.transform, false);
        dot.GetComponent<RectTransform>().sizeDelta = new Vector2(10f, 10f);
        LayoutElement dotLayout = dot.GetComponent<LayoutElement>();
        dotLayout.minWidth = 12f;
        dotLayout.preferredWidth = 12f;
        dotLayout.minHeight = 10f;
        dotLayout.preferredHeight = 10f;
        dot.GetComponent<Image>().color = new Color(0.15f, 0.68f, 0.38f);

        GameObject info = new GameObject("InfoGroup", typeof(RectTransform), typeof(VerticalLayoutGroup), typeof(LayoutElement));
        info.transform.SetParent(content.transform, false);
        LayoutElement infoLayout = info.GetComponent<LayoutElement>();
        infoLayout.minWidth = 260f;
        infoLayout.preferredWidth = 420f;
        infoLayout.flexibleWidth = 1f;
        VerticalLayoutGroup infoVlg = info.GetComponent<VerticalLayoutGroup>();
        infoVlg.spacing = 4f;
        infoVlg.childControlWidth = true;
        infoVlg.childControlHeight = false;
        infoVlg.childForceExpandWidth = true;
        infoVlg.childForceExpandHeight = false;

        CreateRuntimeText("LabNameText", info.transform, "实验室名称", 15, FontStyles.Bold, new Color(0.93f, 0.95f, 0.98f), new Vector2(0f, 22f));
        GameObject subRow = new GameObject("SubRow", typeof(RectTransform), typeof(HorizontalLayoutGroup));
        subRow.transform.SetParent(info.transform, false);
        subRow.GetComponent<RectTransform>().sizeDelta = new Vector2(0f, 20f);
        HorizontalLayoutGroup subLayout = subRow.GetComponent<HorizontalLayoutGroup>();
        subLayout.spacing = 10f;
        subLayout.childControlWidth = true;
        subLayout.childControlHeight = true;
        subLayout.childForceExpandWidth = false;
        subLayout.childForceExpandHeight = true;
        CreateRuntimeText("LocationText", subRow.transform, "位置", 12, FontStyles.Normal, new Color(0.62f, 0.67f, 0.76f), new Vector2(230f, 20f));
        CreateRuntimeText("LabCodeText", subRow.transform, "编号", 12, FontStyles.Normal, new Color(0.50f, 0.56f, 0.66f), new Vector2(150f, 20f));

        GameObject pill = new GameObject("StatusBadgePill", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(LayoutElement));
        pill.transform.SetParent(content.transform, false);
        pill.GetComponent<RectTransform>().sizeDelta = new Vector2(86f, 30f);
        LayoutElement pillLayout = pill.GetComponent<LayoutElement>();
        pillLayout.minWidth = 86f;
        pillLayout.preferredWidth = 86f;
        pill.GetComponent<Image>().color = new Color(0.15f, 0.68f, 0.38f, 0.18f);
        TextMeshProUGUI status = CreateRuntimeText("StatusText", pill.transform, "开放中", 12, FontStyles.Bold, new Color(0.15f, 0.68f, 0.38f), Vector2.zero);
        RectTransform statusRt = status.GetComponent<RectTransform>();
        statusRt.anchorMin = Vector2.zero;
        statusRt.anchorMax = Vector2.one;
        statusRt.offsetMin = Vector2.zero;
        statusRt.offsetMax = Vector2.zero;
        status.alignment = TextAlignmentOptions.Center;

        GameObject buttonObject = new GameObject("ViewDetailButton", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(Button), typeof(LayoutElement));
        buttonObject.transform.SetParent(content.transform, false);
        buttonObject.GetComponent<RectTransform>().sizeDelta = new Vector2(110f, 34f);
        LayoutElement buttonLayout = buttonObject.GetComponent<LayoutElement>();
        buttonLayout.minWidth = 110f;
        buttonLayout.preferredWidth = 110f;
        Image buttonImage = buttonObject.GetComponent<Image>();
        buttonImage.color = new Color(0.13f, 0.38f, 0.74f);
        buttonObject.GetComponent<Button>().targetGraphic = buttonImage;
        TextMeshProUGUI buttonText = CreateRuntimeText("Text", buttonObject.transform, "查看详情", 12, FontStyles.Bold, Color.white, Vector2.zero);
        RectTransform buttonTextRt = buttonText.GetComponent<RectTransform>();
        buttonTextRt.anchorMin = Vector2.zero;
        buttonTextRt.anchorMax = Vector2.one;
        buttonTextRt.offsetMin = Vector2.zero;
        buttonTextRt.offsetMax = Vector2.zero;
        buttonText.alignment = TextAlignmentOptions.Center;

        return row;
    }

    private TextMeshProUGUI CreateRuntimeText(string name, Transform parent, string text, int size, FontStyles style, Color color, Vector2 sizeDelta)
    {
        TMP_FontAsset font = ResolveRuntimeFont();
        GameObject go = new GameObject(name, typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI), typeof(LayoutElement));
        go.transform.SetParent(parent, false);
        RectTransform rt = go.GetComponent<RectTransform>();
        rt.sizeDelta = sizeDelta;
        LayoutElement layout = go.GetComponent<LayoutElement>();
        if (sizeDelta.x > 0f)
        {
            layout.minWidth = sizeDelta.x;
            layout.preferredWidth = sizeDelta.x;
        }
        if (sizeDelta.y > 0f)
        {
            layout.minHeight = sizeDelta.y;
            layout.preferredHeight = sizeDelta.y;
        }
        TextMeshProUGUI tmp = go.GetComponent<TextMeshProUGUI>();
        tmp.font = font;
        tmp.text = text;
        tmp.fontSize = size;
        tmp.fontStyle = style;
        tmp.color = color;
        tmp.alignment = TextAlignmentOptions.Left;
        tmp.raycastTarget = false;
        tmp.enableWordWrapping = false;
        tmp.overflowMode = TextOverflowModes.Ellipsis;
        return tmp;
    }

    private TMP_FontAsset ResolveRuntimeFont()
    {
        if (IsUsableChineseFont(runtimeFont))
            return runtimeFont;

        TextMeshProUGUI[] preferredTexts =
        {
            titleText,
            overviewSummaryText,
            detailLabNameText,
            detailLabCodeText,
            detailStatusText,
            detailLocationText,
            detailDescriptionText,
            detailCapacityText,
            detailAreaText,
            detailDutyPersonText,
            detailEquipmentText,
            loadingText
        };

        for (int i = 0; i < preferredTexts.Length; i++)
        {
            if (preferredTexts[i] != null && IsUsableChineseFont(preferredTexts[i].font))
            {
                runtimeFont = preferredTexts[i].font;
                return runtimeFont;
            }
        }

        TextMeshProUGUI[] localTexts = GetComponentsInChildren<TextMeshProUGUI>(true);
        for (int i = 0; i < localTexts.Length; i++)
        {
            if (localTexts[i] != null && IsUsableChineseFont(localTexts[i].font) && !IsRuntimeRowText(localTexts[i].transform))
            {
                runtimeFont = localTexts[i].font;
                return runtimeFont;
            }
        }

        runtimeFont = TMP_Settings.defaultFontAsset;
        return runtimeFont;
    }

    private bool IsUsableChineseFont(TMP_FontAsset font)
    {
        return font != null && font.HasCharacter('实', true);
    }

    private bool IsRuntimeRowText(Transform target)
    {
        Transform cursor = target;
        while (cursor != null && cursor != transform)
        {
            if (cursor.name.StartsWith("LabListRow_Runtime"))
                return true;
            cursor = cursor.parent;
        }

        return false;
    }

    private void SetupCard(GameObject card, LabInfoData lab)
    {
        // 名称（路径：RowContent/InfoGroup/LabNameText）
        var nameText = card.transform.Find("RowContent/InfoGroup/LabNameText")?.GetComponent<TextMeshProUGUI>();
        if (nameText != null) nameText.text = lab.labName;

        // 地点（路径：RowContent/InfoGroup/SubRow/LocationText）
        var locationText = card.transform.Find("RowContent/InfoGroup/SubRow/LocationText")?.GetComponent<TextMeshProUGUI>();
        if (locationText != null) locationText.text = lab.FullLocation;

        // 编号（路径：RowContent/InfoGroup/SubRow/LabCodeText）
        var codeText = card.transform.Find("RowContent/InfoGroup/SubRow/LabCodeText")?.GetComponent<TextMeshProUGUI>();
        if (codeText != null) codeText.text = lab.labCode;

        // 状态徽章文字 + 颜色（路径：RowContent/StatusBadgePill/StatusText）
        var statusText = card.transform.Find("RowContent/StatusBadgePill/StatusText")?.GetComponent<TextMeshProUGUI>();
        var badgeBg = card.transform.Find("RowContent/StatusBadgePill")?.GetComponent<Image>();
        if (ColorUtility.TryParseHtmlString(lab.StatusColor, out Color sc))
        {
            if (statusText != null) { statusText.text = lab.StatusName; statusText.color = sc; }
            if (badgeBg != null) badgeBg.color = new Color(sc.r, sc.g, sc.b, 0.18f);
        }

        // 左侧状态色条（路径：AccentBar）
        var accentBar = card.transform.Find("AccentBar")?.GetComponent<Image>();
        if (accentBar != null && ColorUtility.TryParseHtmlString(lab.StatusColor, out Color ac))
            accentBar.color = ac;

        // 状态圆点（路径：RowContent/StatusBadge）
        var dot = card.transform.Find("RowContent/StatusBadge")?.GetComponent<Image>();
        if (dot != null && ColorUtility.TryParseHtmlString(lab.StatusColor, out Color dc))
            dot.color = dc;

        // 查看按钮（路径：RowContent/ViewDetailButton）
        var viewBtn = card.transform.Find("RowContent/ViewDetailButton")?.GetComponent<Button>();
        if (viewBtn != null)
        {
            viewBtn.onClick.RemoveAllListeners();
            LabInfoData captured = lab;
            viewBtn.onClick.AddListener(() => ShowDetailOverlay(captured));
        }
    }

    /// <summary>
    /// Tab 切换
    /// </summary>
    public void SwitchTab(int tabIndex)
    {
        currentTab = tabIndex;
        bool isOverview = tabIndex == 0;

        // 切换面板显示
        if (overviewPanel != null) overviewPanel.SetActive(isOverview);
        if (borrowPanel != null) borrowPanel.SetActive(!isOverview);

        // 切换高亮
        if (tabOverviewHighlight != null) tabOverviewHighlight.SetActive(isOverview);
        if (tabBorrowHighlight != null) tabBorrowHighlight.SetActive(!isOverview);

        // 切换按钮颜色
        SetTabButtonColor(tabOverviewButton, isOverview);
        SetTabButtonColor(tabBorrowButton, !isOverview);
    }

    private void SetTabButtonColor(Button btn, bool active)
    {
        if (btn == null) return;
        var colors = btn.colors;
        colors.normalColor = active ? tabActiveColor : tabInactiveColor;
        btn.colors = colors;

        if (btn.targetGraphic is Graphic graphic)
        {
            graphic.color = active ? tabActiveColor : tabInactiveColor;
        }

        // 同时设置按钮内文字颜色
        var label = btn.GetComponentInChildren<TextMeshProUGUI>();
        if (label != null)
        {
            label.color = active ? new Color(0.95f, 0.97f, 1f) : new Color(0.72f, 0.76f, 0.82f);
        }
    }

    private void BindButtonEvents()
    {
        if (closeButton != null)
        {
            closeButton.onClick.RemoveAllListeners();
            closeButton.onClick.AddListener(ClosePanel);
        }

        if (tabOverviewButton != null)
        {
            tabOverviewButton.onClick.RemoveAllListeners();
            tabOverviewButton.onClick.AddListener(() => SwitchTab(0));
        }

        if (tabBorrowButton != null)
        {
            tabBorrowButton.onClick.RemoveAllListeners();
            tabBorrowButton.onClick.AddListener(() => SwitchTab(1));
        }

        if (detailOverlayButton != null)
        {
            detailOverlayButton.onClick.RemoveAllListeners();
            detailOverlayButton.onClick.AddListener(HideDetailOverlay);
        }

        if (detailCloseButton != null)
        {
            detailCloseButton.onClick.RemoveAllListeners();
            detailCloseButton.onClick.AddListener(HideDetailOverlay);
        }

        if (detailBorrowButton != null)
        {
            detailBorrowButton.onClick.RemoveAllListeners();
            detailBorrowButton.onClick.AddListener(OpenBorrowForCurrentLab);
        }
    }

    public void ShowDetailOverlay(LabInfoData lab)
    {
        if (lab == null)
        {
            Debug.LogWarning("[LabInfoPanelUI] ShowDetailOverlay: lab 为 null");
            return;
        }
        if (detailOverlay == null)
        {
            Debug.LogError("[LabInfoPanelUI] ShowDetailOverlay: detailOverlay 未绑定！请重新运行 Tools → 搞建大屏交互 UI");
            return;
        }

        Debug.Log($"[LabInfoPanelUI] ShowDetailOverlay: {lab.labName}");
        currentDetailLab = lab;

        if (detailLabNameText != null) detailLabNameText.text = lab.labName;
        if (detailLabCodeText != null) detailLabCodeText.text = lab.labCode;
        if (detailStatusText != null) detailStatusText.text = lab.StatusName;
        if (detailLocationText != null) detailLocationText.text = $"📍 {lab.FullLocation}";
        if (detailDescriptionText != null) detailDescriptionText.text = lab.description;
        if (detailCapacityText != null) detailCapacityText.text = $"{lab.capacity} 人";
        if (detailAreaText != null) detailAreaText.text = $"{lab.areaM2} m²";
        if (detailDutyPersonText != null) detailDutyPersonText.text = lab.dutyUserName;
        if (detailEquipmentText != null) detailEquipmentText.text = lab.equipment;

        if (detailStatusBadge != null && ColorUtility.TryParseHtmlString(lab.StatusColor, out Color statusColor))
            detailStatusBadge.color = statusColor;

        // 确保 overlay 在最上层渲染（在 OverviewPanel 中显示在最前）
        detailOverlay.transform.SetAsLastSibling();
        detailOverlay.SetActive(true);
    }

    public void HideDetailOverlay()
    {
        if (detailOverlay != null)
        {
            detailOverlay.SetActive(false);
        }

        currentDetailLab = null;
    }

    private void OpenBorrowForCurrentLab()
    {
        if (currentDetailLab == null)
        {
            return;
        }

        HideDetailOverlay();
        SwitchTab(1);

        if (borrowFormController != null)
        {
            borrowFormController.RefreshLabList(currentLabs);
            borrowFormController.SelectLabById(currentDetailLab.id);
        }
    }

    /// <summary>
    /// 关闭面板
    /// </summary>
    public void ClosePanel()
    {
        // 恢复玩家移动和视角
        PlayerMove pm = FindObjectOfType<PlayerMove>(true);
        if (pm != null) pm.inputEnabled = true;

        ScreenTrigger matchedTrigger = null;
        foreach (ScreenTrigger trigger in FindObjectsOfType<ScreenTrigger>(true))
        {
            if (trigger != null && trigger.labInfoPanel == gameObject)
            {
                matchedTrigger = trigger;
                break;
            }
        }

        if (matchedTrigger != null)
        {
            gameObject.SetActive(false);
            Cursor.lockState = CursorLockMode.Locked;
            Cursor.visible = false;
            matchedTrigger.NotifyPanelClosed();
        }
        else
        {
            gameObject.SetActive(false);
            Cursor.lockState = CursorLockMode.Locked;
            Cursor.visible = false;
        }
    }

    // ==================== 加载状态 ====================

    public void ShowLoading(string message = "加载中...")
    {
        if (loadingOverlay != null)
        {
            loadingOverlay.SetActive(true);
            if (loadingText != null) loadingText.text = message;
        }
    }

    public void HideLoading()
    {
        if (loadingOverlay != null) loadingOverlay.SetActive(false);
    }
}
