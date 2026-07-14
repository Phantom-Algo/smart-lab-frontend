using TMPro;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public static class SmartLabDropdownRuntimeFix
{
    private const float ItemHeight = 30f;
    private static readonly Color ItemTextColor = new Color(0.91f, 0.93f, 0.97f, 1f);
    private static readonly Color ItemHighlightColor = new Color(0.13f, 0.26f, 0.46f, 0.72f);

    public static void Normalize(TMP_Dropdown dropdown)
    {
        if (dropdown == null || dropdown.template == null)
            return;

        SmartLabDropdownListWatcher watcher = dropdown.GetComponent<SmartLabDropdownListWatcher>();
        if (watcher == null)
            watcher = dropdown.gameObject.AddComponent<SmartLabDropdownListWatcher>();
        watcher.Bind(dropdown);

        SmartLabSimpleDropdownFallback fallback = dropdown.GetComponent<SmartLabSimpleDropdownFallback>();
        if (fallback == null)
            fallback = dropdown.gameObject.AddComponent<SmartLabSimpleDropdownFallback>();
        fallback.Bind(dropdown);

        // 由 SmartLabSimpleDropdownFallback 接管展开/选择，避免 TMP_Dropdown 原生列表再次弹出空白背景。
        dropdown.enabled = false;

        RectTransform templateRt = dropdown.template;
        templateRt.gameObject.SetActive(false);
        Transform viewport = templateRt.Find("Viewport");
        Transform content = viewport != null ? viewport.Find("Content") : null;
        Transform item = content != null ? content.Find("Item") : null;
        if (viewport == null || content == null || item == null)
            return;

        RectTransform contentRt = content as RectTransform;
        if (contentRt != null)
        {
            contentRt.anchorMin = new Vector2(0f, 1f);
            contentRt.anchorMax = new Vector2(1f, 1f);
            contentRt.pivot = new Vector2(0.5f, 1f);
            contentRt.anchoredPosition = Vector2.zero;
            float preferredHeight = Mathf.Max(ItemHeight, dropdown.options.Count * ItemHeight);
            contentRt.sizeDelta = new Vector2(0f, preferredHeight);

            VerticalLayoutGroup layoutGroup = contentRt.GetComponent<VerticalLayoutGroup>();
            if (layoutGroup == null)
                layoutGroup = contentRt.gameObject.AddComponent<VerticalLayoutGroup>();
            layoutGroup.enabled = true;
            layoutGroup.spacing = 0f;
            layoutGroup.childAlignment = TextAnchor.UpperLeft;
            layoutGroup.childControlWidth = true;
            layoutGroup.childControlHeight = true;
            layoutGroup.childForceExpandWidth = true;
            layoutGroup.childForceExpandHeight = false;

            ContentSizeFitter fitter = contentRt.GetComponent<ContentSizeFitter>();
            if (fitter == null)
                fitter = contentRt.gameObject.AddComponent<ContentSizeFitter>();
            fitter.enabled = true;
            fitter.horizontalFit = ContentSizeFitter.FitMode.Unconstrained;
            fitter.verticalFit = ContentSizeFitter.FitMode.PreferredSize;
        }

        RectTransform itemRt = item as RectTransform;
        if (itemRt != null)
        {
            itemRt.anchorMin = new Vector2(0f, 0.5f);
            itemRt.anchorMax = new Vector2(1f, 0.5f);
            itemRt.pivot = new Vector2(0.5f, 0.5f);
            itemRt.anchoredPosition = Vector2.zero;
            itemRt.sizeDelta = new Vector2(0f, ItemHeight);

            LayoutElement layoutElement = itemRt.GetComponent<LayoutElement>();
            if (layoutElement == null)
                layoutElement = itemRt.gameObject.AddComponent<LayoutElement>();

            layoutElement.minHeight = ItemHeight;
            layoutElement.preferredHeight = ItemHeight;
            layoutElement.flexibleWidth = 1f;
        }

        item.gameObject.SetActive(true);
        Toggle itemToggle = item.GetComponent<Toggle>();
        if (itemToggle == null)
            itemToggle = item.gameObject.AddComponent<Toggle>();

        Transform itemBackground = item.Find("Item Background");
        RectTransform itemLabelRt = item.Find("Item Label") as RectTransform;
        RectTransform itemCheckRt = item.Find("Item Checkmark") as RectTransform;

        if (itemBackground == null)
        {
            GameObject backgroundObject = new GameObject("Item Background", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image));
            backgroundObject.transform.SetParent(item, false);
            itemBackground = backgroundObject.transform;
        }

        Image itemBackgroundImage = itemBackground.GetComponent<Image>();
        if (itemBackgroundImage == null)
            itemBackgroundImage = itemBackground.gameObject.AddComponent<Image>();
        itemBackgroundImage.color = new Color(0f, 0f, 0f, 0f);
        itemBackgroundImage.raycastTarget = true;
        itemToggle.targetGraphic = itemBackgroundImage;

        if (itemLabelRt != null)
        {
            if (itemLabelRt.parent != item)
                itemLabelRt.SetParent(item, false);
            itemLabelRt.anchorMin = new Vector2(0f, 0f);
            itemLabelRt.anchorMax = new Vector2(1f, 1f);
            itemLabelRt.offsetMin = new Vector2(28f, 0f);
            itemLabelRt.offsetMax = new Vector2(-10f, 0f);

            TextMeshProUGUI itemLabel = itemLabelRt.GetComponent<TextMeshProUGUI>();
            if (itemLabel != null)
            {
                itemLabel.color = ItemTextColor;
                itemLabel.alignment = TextAlignmentOptions.Left;
                itemLabel.raycastTarget = false;
                dropdown.itemText = itemLabel;
            }
        }

        if (itemCheckRt != null)
        {
            if (itemCheckRt.parent != item)
                itemCheckRt.SetParent(item, false);
            itemCheckRt.anchorMin = new Vector2(0f, 0f);
            itemCheckRt.anchorMax = new Vector2(0f, 1f);
            itemCheckRt.pivot = new Vector2(0f, 0.5f);
            itemCheckRt.sizeDelta = new Vector2(14f, 0f);
            itemCheckRt.anchoredPosition = new Vector2(10f, 0f);

            Image itemCheckImage = itemCheckRt.GetComponent<Image>();
            if (itemCheckImage != null)
            {
                itemCheckImage.color = ItemHighlightColor;
                itemCheckImage.raycastTarget = false;
                itemToggle.graphic = itemCheckImage;
                dropdown.itemImage = itemCheckImage;
            }
        }

        dropdown.RefreshShownValue();
        LayoutRebuilder.ForceRebuildLayoutImmediate(content as RectTransform);
    }

    public static void NormalizeGeneratedList(TMP_Dropdown dropdown, RectTransform listRt)
    {
        Transform viewport = listRt.Find("Viewport");
        Transform content = viewport != null ? viewport.Find("Content") : null;
        if (content == null)
            return;

        RectTransform contentRt = content as RectTransform;
        if (contentRt != null)
        {
            contentRt.anchorMin = new Vector2(0f, 1f);
            contentRt.anchorMax = new Vector2(1f, 1f);
            contentRt.pivot = new Vector2(0.5f, 1f);
            contentRt.anchoredPosition = Vector2.zero;
            contentRt.sizeDelta = new Vector2(0f, Mathf.Max(ItemHeight, dropdown.options.Count * ItemHeight));
        }

        Toggle[] toggles = listRt.GetComponentsInChildren<Toggle>(true);
        foreach (Toggle toggle in toggles)
        {
            toggle.gameObject.SetActive(true);
            toggle.interactable = true;

            RectTransform itemRt = toggle.GetComponent<RectTransform>();
            if (itemRt != null)
            {
                itemRt.anchorMin = new Vector2(0f, 0.5f);
                itemRt.anchorMax = new Vector2(1f, 0.5f);
                itemRt.pivot = new Vector2(0.5f, 0.5f);
                itemRt.sizeDelta = new Vector2(0f, ItemHeight);
            }

            LayoutElement layoutElement = toggle.GetComponent<LayoutElement>();
            if (layoutElement == null)
                layoutElement = toggle.gameObject.AddComponent<LayoutElement>();
            layoutElement.minHeight = ItemHeight;
            layoutElement.preferredHeight = ItemHeight;
            layoutElement.flexibleWidth = 1f;

            Image background = toggle.targetGraphic as Image;
            if (background != null)
                background.raycastTarget = true;

            foreach (TextMeshProUGUI label in toggle.GetComponentsInChildren<TextMeshProUGUI>(true))
            {
                label.gameObject.SetActive(true);
                label.color = ItemTextColor;
                label.alignment = TextAlignmentOptions.Left;
                label.raycastTarget = false;

                RectTransform labelRt = label.GetComponent<RectTransform>();
                if (labelRt != null)
                {
                    labelRt.anchorMin = new Vector2(0f, 0f);
                    labelRt.anchorMax = new Vector2(1f, 1f);
                    labelRt.offsetMin = new Vector2(28f, 0f);
                    labelRt.offsetMax = new Vector2(-10f, 0f);
                }
            }
        }

        if (contentRt != null)
            LayoutRebuilder.ForceRebuildLayoutImmediate(contentRt);
    }
}

public sealed class SmartLabDropdownListWatcher : MonoBehaviour, IPointerClickHandler
{
    private TMP_Dropdown dropdown;
    private RectTransform lastList;

    public void Bind(TMP_Dropdown target)
    {
        dropdown = target;
    }

    public void OnPointerClick(PointerEventData eventData)
    {
        FixOpenList(force: true);
    }

    private void LateUpdate()
    {
        FixOpenList(force: false);
    }

    private void FixOpenList(bool force)
    {
        if (dropdown == null)
            return;

        Transform list = dropdown.transform.Find("Dropdown List");
        if (list == null)
        {
            lastList = null;
            return;
        }

        RectTransform listRt = list as RectTransform;
        if (listRt == null)
            return;

        if (!force && lastList == listRt)
            return;

        lastList = listRt;
        SmartLabDropdownRuntimeFix.NormalizeGeneratedList(dropdown, listRt);
    }
}

public sealed class SmartLabSimpleDropdownFallback : MonoBehaviour, IPointerClickHandler
{
    private const float ItemHeight = 34f;
    private TMP_Dropdown dropdown;
    private GameObject popup;
    private TMP_FontAsset fallbackFont;

    public void Bind(TMP_Dropdown target)
    {
        dropdown = target;
        fallbackFont = ResolveFont();
    }

    public void OnPointerClick(PointerEventData eventData)
    {
        if (dropdown == null || !dropdown.interactable || dropdown.options == null || dropdown.options.Count == 0)
            return;

        if (popup != null)
        {
            Destroy(popup);
            popup = null;
            HideNativeDropdownList();
            return;
        }

        ShowPopup();
    }

    private void OnDisable()
    {
        if (popup != null)
        {
            Destroy(popup);
            popup = null;
        }
    }

    private void ShowPopup()
    {
        Transform parent = ResolvePopupParent();
        popup = new GameObject($"{dropdown.gameObject.name}_SimpleDropdown", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(VerticalLayoutGroup), typeof(LayoutElement));
        popup.transform.SetParent(parent, false);
        popup.transform.SetAsLastSibling();
        popup.GetComponent<LayoutElement>().ignoreLayout = true;

        RectTransform dropdownRt = dropdown.GetComponent<RectTransform>();
        RectTransform popupRt = popup.GetComponent<RectTransform>();
        popupRt.anchorMin = new Vector2(0f, 1f);
        popupRt.anchorMax = new Vector2(0f, 1f);
        popupRt.pivot = new Vector2(0f, 1f);
        popupRt.sizeDelta = new Vector2(dropdownRt.rect.width > 0f ? dropdownRt.rect.width : dropdownRt.sizeDelta.x,
            Mathf.Min(240f, Mathf.Max(ItemHeight, dropdown.options.Count * ItemHeight)));
        AlignPopupToDropdown(popupRt);

        Image popupImage = popup.GetComponent<Image>();
        popupImage.color = new Color(0.12f, 0.16f, 0.25f, 0.98f);
        popupImage.raycastTarget = true;

        VerticalLayoutGroup layout = popup.GetComponent<VerticalLayoutGroup>();
        layout.spacing = 0f;
        layout.padding = new RectOffset(0, 0, 2, 2);
        layout.childAlignment = TextAnchor.UpperLeft;
        layout.childControlWidth = true;
        layout.childControlHeight = false;
        layout.childForceExpandWidth = true;
        layout.childForceExpandHeight = false;

        for (int i = 0; i < dropdown.options.Count; i++)
        {
            int capturedIndex = i;
            string optionText = dropdown.options[i].text;
            GameObject item = CreatePopupItem(optionText, capturedIndex == dropdown.value);
            item.transform.SetParent(popup.transform, false);
            Button button = item.GetComponent<Button>();
            button.onClick.AddListener(() => Select(capturedIndex));
        }

        Canvas.ForceUpdateCanvases();
        LayoutRebuilder.ForceRebuildLayoutImmediate(popupRt);
        HideNativeDropdownList();
    }

    private GameObject CreatePopupItem(string text, bool selected)
    {
        GameObject item = new GameObject("Option", typeof(RectTransform), typeof(CanvasRenderer), typeof(Image), typeof(Button), typeof(LayoutElement));
        RectTransform itemRt = item.GetComponent<RectTransform>();
        itemRt.sizeDelta = new Vector2(0f, ItemHeight);
        LayoutElement layout = item.GetComponent<LayoutElement>();
        layout.minHeight = ItemHeight;
        layout.preferredHeight = ItemHeight;
        layout.flexibleWidth = 1f;

        Image image = item.GetComponent<Image>();
        image.color = selected ? new Color(0.13f, 0.32f, 0.58f, 0.88f) : new Color(0.08f, 0.11f, 0.17f, 0.72f);
        image.raycastTarget = true;

        Button button = item.GetComponent<Button>();
        button.targetGraphic = image;

        GameObject labelObject = new GameObject("Label", typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI));
        labelObject.transform.SetParent(item.transform, false);
        RectTransform labelRt = labelObject.GetComponent<RectTransform>();
        labelRt.anchorMin = Vector2.zero;
        labelRt.anchorMax = Vector2.one;
        labelRt.offsetMin = new Vector2(14f, 0f);
        labelRt.offsetMax = new Vector2(-12f, 0f);

        TextMeshProUGUI label = labelObject.GetComponent<TextMeshProUGUI>();
        label.text = text;
        label.font = fallbackFont != null ? fallbackFont : ResolveFont();
        label.fontSize = 13f;
        label.fontStyle = FontStyles.Bold;
        label.color = new Color(0.93f, 0.95f, 0.98f, 1f);
        label.alignment = TextAlignmentOptions.Left;
        label.raycastTarget = false;
        label.enableWordWrapping = false;

        return item;
    }

    private void Select(int index)
    {
        if (dropdown == null)
            return;

        dropdown.SetValueWithoutNotify(index);
        dropdown.RefreshShownValue();
        dropdown.onValueChanged.Invoke(index);

        if (popup != null)
        {
            Destroy(popup);
            popup = null;
        }
    }

    private void LateUpdate()
    {
        if (popup != null)
        {
            RectTransform popupRt = popup.GetComponent<RectTransform>();
            if (popupRt != null)
                AlignPopupToDropdown(popupRt);
        }

        HideNativeDropdownList();
    }

    private Transform ResolvePopupParent()
    {
        Canvas canvas = dropdown != null ? dropdown.GetComponentInParent<Canvas>() : null;
        if (canvas != null)
            return canvas.transform;

        return dropdown != null && dropdown.transform.parent != null
            ? dropdown.transform.parent
            : dropdown.transform;
    }

    private void AlignPopupToDropdown(RectTransform popupRt)
    {
        if (dropdown == null || popupRt == null)
            return;

        RectTransform dropdownRt = dropdown.GetComponent<RectTransform>();
        if (dropdownRt == null)
            return;

        float width = dropdownRt.rect.width > 0f ? dropdownRt.rect.width : dropdownRt.sizeDelta.x;
        popupRt.SetSizeWithCurrentAnchors(RectTransform.Axis.Horizontal, width);

        Vector3[] corners = new Vector3[4];
        dropdownRt.GetWorldCorners(corners);
        popupRt.position = corners[0] + Vector3.down * 4f;
    }

    private void HideNativeDropdownList()
    {
        if (dropdown == null)
            return;

        Transform nativeList = dropdown.transform.Find("Dropdown List");
        if (nativeList != null)
            nativeList.gameObject.SetActive(false);
    }

    private TMP_FontAsset ResolveFont()
    {
        if (dropdown != null && dropdown.captionText != null && dropdown.captionText.font != null)
            return dropdown.captionText.font;

        TextMeshProUGUI parentText = GetComponentInParent<TextMeshProUGUI>();
        if (parentText != null && parentText.font != null)
            return parentText.font;

        return TMP_Settings.defaultFontAsset;
    }
}
