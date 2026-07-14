using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

/// <summary>
/// 实验室基础数据模型
/// </summary>
[Serializable]
public class LabInfoData
{
    public int id;
    public string labCode;          // 实验室编码，如 LAB-2024-A305
    public string labName;          // 实验室名称
    public string labTypeName;      // 实验室类型名称
    public string campus;           // 校区
    public string building;         // 楼栋
    public int floor;               // 楼层
    public string roomNo;           // 房间号
    public int capacity;            // 容纳人数
    public float areaM2;            // 面积（平方米）
    public string dutyUserName;     // 负责人姓名
    public int runningStatus;       // 运行状态：1=开放中, 2=维护中, 3=已关闭
    public string equipment;        // 主要设备描述
    public string description;      // 实验室描述

    /// <summary>格式化后的完整位置信息</summary>
    public string FullLocation => $"{campus} · {building} {floor}F {roomNo}";

    /// <summary>运行状态中文名</summary>
    public string StatusName => runningStatus switch
    {
        1 => "开放中",
        2 => "维护中",
        _ => "已关闭"
    };

    /// <summary>状态对应的颜色码</summary>
    public string StatusColor => runningStatus switch
    {
        1 => "#27AE60",
        2 => "#F39C12",
        _ => "#E74C3C"
    };
}

/// <summary>
/// 实验室借用申请表单数据
/// </summary>
[Serializable]
public class LabBorrowRequest
{
    public int labId;               // 目标实验室 ID
    public string applicantName;    // 申请人姓名
    public string reserveDate;      // 预约日期，格式 yyyy-MM-dd
    public string timeSlot;         // 时段，如 "08:00-10:00"
    public string purpose;          // 用途说明
    public int peopleCount;         // 使用人数
}

/// <summary>
/// 实验室数据提供者（单例）
/// 后续可替换为从后端 API 获取数据
/// </summary>
public class LabDataProvider : MonoBehaviour
{
    public static LabDataProvider Instance { get; private set; }

    /// <summary>当前场景类型（在 Inspector 中设置）</summary>
    [Header("当前场景设置")]
    public string currentSceneLabType = "教学实验室";

    /// <summary>Mock 数据列表（后续替换为 API 调用）</summary>
    public List<LabInfoData> mockLabData = new List<LabInfoData>();

    private Coroutine backendLoadCoroutine;

    private void Awake()
    {
        // 不使用 DontDestroyOnLoad（该对象是场景子对象，DontDestroyOnLoad 对子对象无效）
        // LabInfoPanelUI 通过序列化 dataProvider 字段直接引用，不依赖 Instance
        if (Instance == null)
        {
            Instance = this;
        }

        // 如果未在 Inspector 中配置 Mock 数据，则自动生成
        if (mockLabData == null || mockLabData.Count == 0)
        {
            GenerateMockData();
        }
    }

    private void OnEnable()
    {
        WebGLBridge.ContextChanged += HandleWebContextChanged;
        WebGLBridge.RealtimeEventReceived += HandleRealtimeEvent;
    }

    private void Start()
    {
        TryLoadFromBackend();
    }

    private void OnDisable()
    {
        WebGLBridge.ContextChanged -= HandleWebContextChanged;
        WebGLBridge.RealtimeEventReceived -= HandleRealtimeEvent;
    }

    /// <summary>
    /// 获取与本场景类型匹配的实验室列表
    /// </summary>
    public List<LabInfoData> GetLabsByType(string labTypeName)
    {
        return mockLabData.FindAll(l => l.labTypeName == labTypeName);
    }

    /// <summary>
    /// 获取所有实验室
    /// </summary>
    public List<LabInfoData> GetAllLabs()
    {
        return mockLabData;
    }

    /// <summary>
    /// 提交借用申请：优先走后端 API，失败时自动回退到本地模拟
    /// </summary>
    public void SubmitBorrowRequest(LabBorrowRequest request, Action<bool, string> callback)
    {
        if (WebGLBridge.HasUsableContext)
        {
            StartCoroutine(SubmitBorrowRequestWithFallback(request, callback));
            return;
        }

        Debug.Log($"[LabDataProvider] 收到借用申请: LabId={request.labId}, Date={request.reserveDate}, Slot={request.timeSlot}, People={request.peopleCount}, Purpose={request.purpose}");

        // 本地模式：模拟 API 调用延迟
        StartCoroutine(SimulateApiCall(request, callback));
    }

    /// <summary>
    /// 先尝试提交到后端，如果后端返回错误则回退到本地模拟
    /// </summary>
    private IEnumerator SubmitBorrowRequestWithFallback(LabBorrowRequest request, Action<bool, string> callback)
    {
        bool backendSucceeded = false;
        string backendMessage = "";
        bool backendFinished = false;

        StartCoroutine(SubmitBorrowRequestToBackend(request, (success, message) =>
        {
            backendSucceeded = success;
            backendMessage = message;
            backendFinished = true;
        }));

        // 等待后端响应（最多 8 秒）
        float waitTime = 0f;
        while (!backendFinished && waitTime < 8f)
        {
            yield return new WaitForSeconds(0.1f);
            waitTime += 0.1f;
        }

        if (backendSucceeded)
        {
            callback?.Invoke(true, backendMessage);
            yield break;
        }

        string message = string.IsNullOrEmpty(backendMessage) ? "后端预约请求超时" : backendMessage;
        Debug.LogWarning($"[LabDataProvider] 后端预约失败: {message}");
        callback?.Invoke(false, $"预约提交失败：{message}");
    }

    private void HandleWebContextChanged(SmartLabWebContext _)
    {
        TryLoadFromBackend();
    }

    private void HandleRealtimeEvent(SmartLabRealtimeMessage message)
    {
        if (message == null || string.IsNullOrEmpty(message.eventName))
            return;

        if (message.eventName.StartsWith("lab.", StringComparison.OrdinalIgnoreCase) ||
            message.eventName.StartsWith("reservation.", StringComparison.OrdinalIgnoreCase))
        {
            ScheduleBackendReload();
        }
    }

    private void TryLoadFromBackend()
    {
        if (!WebGLBridge.HasUsableContext)
            return;

        StartBackendLoad();
    }

    public void ForceBackendRefresh()
    {
        if (WebGLBridge.HasUsableContext)
        {
            StartBackendLoad();
            return;
        }

        NotifyPanelsRefreshed();
    }

    public void LoadReservationTimeSlots(int labId, string reserveDate, Action<List<string>, string> callback)
    {
        if (!WebGLBridge.HasUsableContext || labId <= 0 || string.IsNullOrWhiteSpace(reserveDate))
        {
            callback?.Invoke(GetDefaultReservationTimeSlots(), null);
            return;
        }

        StartCoroutine(LoadReservationTimeSlotsFromBackend(labId, reserveDate, callback));
    }

    private IEnumerator LoadReservationTimeSlotsFromBackend(int labId, string reserveDate, Action<List<string>, string> callback)
    {
        string responseText = null;
        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbGET,
                $"lab-management/labs/{labId}"),
            text => responseText = text,
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage))
        {
            Debug.LogWarning($"[LabDataProvider] 拉取实验室开放时间失败: {errorMessage}");
            callback?.Invoke(GetDefaultReservationTimeSlots(), errorMessage);
            yield break;
        }

        try
        {
            LabDetailApiResponse response = JsonUtility.FromJson<LabDetailApiResponse>(responseText);
            List<string> slots = BuildSlotsFromSchedule(response != null ? response.data : null, reserveDate);
            callback?.Invoke(slots.Count > 0 ? slots : GetDefaultReservationTimeSlots(), null);
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[LabDataProvider] 解析实验室开放时间失败: {ex.Message}");
            callback?.Invoke(GetDefaultReservationTimeSlots(), ex.Message);
        }
    }

    public static List<string> GetDefaultReservationTimeSlots()
    {
        return new List<string>
        {
            "08:00 - 10:00",
            "10:00 - 12:00",
            "14:00 - 16:00",
            "16:00 - 18:00",
            "18:00 - 20:00",
            "20:00 - 22:00"
        };
    }

    private static List<string> BuildSlotsFromSchedule(LabDetailApiRecord labDetail, string reserveDate)
    {
        List<string> slots = new List<string>();
        if (labDetail == null || labDetail.weeklySchedules == null || labDetail.weeklySchedules.Count == 0)
            return slots;

        if (!DateTime.TryParse(reserveDate, out DateTime date))
            return slots;

        int targetDayOfWeek = date.DayOfWeek == DayOfWeek.Sunday ? 7 : (int)date.DayOfWeek;
        LabScheduleApiRecord schedule = labDetail.weeklySchedules.Find(item => item.dayOfWeek == targetDayOfWeek);
        if (schedule == null)
            return slots;

        if (!TryParseBackendTime(schedule.openTime, out TimeSpan openTime) ||
            !TryParseBackendTime(schedule.closeTime, out TimeSpan closeTime) ||
            closeTime <= openTime)
            return slots;

        TimeSpan cursor = openTime;
        TimeSpan step = TimeSpan.FromHours(2);
        while (cursor < closeTime)
        {
            TimeSpan end = cursor + step;
            if (end > closeTime)
                end = closeTime;

            if (end > cursor)
                slots.Add($"{FormatTime(cursor)} - {FormatTime(end)}");

            cursor = end;
        }

        return slots;
    }

    private static bool TryParseBackendTime(string value, out TimeSpan time)
    {
        time = TimeSpan.Zero;
        if (string.IsNullOrWhiteSpace(value))
            return false;

        return TimeSpan.TryParse(value, out time);
    }

    private static string FormatTime(TimeSpan time)
    {
        return $"{(int)time.TotalHours:00}:{time.Minutes:00}";
    }

    private void ScheduleBackendReload(float delaySeconds = 0.35f)
    {
        CancelInvoke(nameof(ReloadFromBackend));
        Invoke(nameof(ReloadFromBackend), delaySeconds);
    }

    private void ReloadFromBackend()
    {
        StartBackendLoad();
    }

    private void StartBackendLoad()
    {
        if (!isActiveAndEnabled)
            return;

        if (backendLoadCoroutine != null)
        {
            StopCoroutine(backendLoadCoroutine);
        }

        backendLoadCoroutine = StartCoroutine(LoadLabsFromBackend());
    }

    private IEnumerator LoadLabsFromBackend()
    {
        string responseText = null;
        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbGET, "lab-management/labs?page=1&pageSize=100"),
            text => responseText = text,
            error => errorMessage = error);

        backendLoadCoroutine = null;

        if (!string.IsNullOrEmpty(errorMessage))
        {
            Debug.LogWarning($"[LabDataProvider] 拉取实验室数据失败: {errorMessage}");
            yield break;
        }

        LabPageApiResponse response = null;
        try
        {
            response = JsonUtility.FromJson<LabPageApiResponse>(responseText);
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[LabDataProvider] 解析实验室列表失败: {ex.Message}");
            yield break;
        }

        if (response == null || response.data == null || response.data.records == null)
        {
            Debug.LogWarning("[LabDataProvider] 实验室列表响应为空，保留当前数据");
            yield break;
        }

        mockLabData = MapLabs(response.data.records);

        // 保留兜底数据中后端没有的类型（如"教学实验室""小组讨论室"等数据库暂无的类型）
        List<LabInfoData> fallback = GetFallbackEntries();
        HashSet<string> backendTypes = new HashSet<string>();
        foreach (var lab in mockLabData) backendTypes.Add(lab.labTypeName);
        foreach (var fb in fallback)
        {
            if (!backendTypes.Contains(fb.labTypeName))
            {
                mockLabData.Add(fb);
                backendTypes.Add(fb.labTypeName);
            }
        }

        NotifyPanelsRefreshed();
        WebGLBridge.EmitWebEvent("scene.lab_data_synced", $"已同步 {mockLabData.Count} 间实验室数据（含兜底）", "success");
    }

    /// <summary>返回后端可能缺失类型的兜底数据（教学实验室、小组讨论室等）</summary>
    private static List<LabInfoData> GetFallbackEntries()
    {
        return new List<LabInfoData>
        {
            new LabInfoData { id = 9001, labCode = "LAB-FB-001", labName = "A301 教学实验室",
                labTypeName = "教学实验室", campus = "东校区", building = "教学楼A",
                floor = 3, roomNo = "A301", capacity = 40, areaM2 = 85f,
                dutyUserName = "王管理员", runningStatus = 1,
                equipment = "多功能实验台×20、投影仪×1、白板×2",
                description = "标准教学实验环境。" },
            new LabInfoData { id = 9002, labCode = "LAB-FB-002", labName = "D101 小组讨论室",
                labTypeName = "小组讨论室", campus = "东校区", building = "教学楼D",
                floor = 1, roomNo = "D101", capacity = 12, areaM2 = 28f,
                dutyUserName = "行政管理员", runningStatus = 1,
                equipment = "会议桌×1、显示屏×1、白板×2",
                description = "小组讨论室。" },
        };
    }

    private IEnumerator SubmitBorrowRequestToBackend(LabBorrowRequest request, Action<bool, string> callback)
    {
        if (request == null)
        {
            callback?.Invoke(false, "预约请求不能为空");
            yield break;
        }

        if (!TryParseTimeSlot(request.timeSlot, out string startTime, out string endTime))
        {
            callback?.Invoke(false, "预约时段格式不正确");
            yield break;
        }

        LabReservationCreateApiRequest payload = new LabReservationCreateApiRequest
        {
            labId = request.labId,
            reservationDate = request.reserveDate,
            startTime = startTime,
            endTime = endTime,
            purpose = BuildReservationPurpose(request),
            courseName = "Unity 3D 场景预约"
        };

        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbPOST,
                "lab-management/reservations",
                JsonUtility.ToJson(payload)),
            _ => { },
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage))
        {
            callback?.Invoke(false, errorMessage);
            yield break;
        }

        ScheduleBackendReload(0.1f);
        WebGLBridge.EmitWebEvent(
            "scene.lab_reservation_submitted",
            $"已提交实验室预约：{request.reserveDate} {request.timeSlot}",
            "success",
            null,
            request.labId.ToString());
        callback?.Invoke(true, "预约申请已提交，请等待管理员审核！");
    }

    private List<LabInfoData> MapLabs(List<LabApiRecord> records)
    {
        List<LabInfoData> labs = new List<LabInfoData>();
        for (int i = 0; i < records.Count; i++)
        {
            LabApiRecord record = records[i];
            labs.Add(new LabInfoData
            {
                id = (int)record.id,
                labCode = ResolveLabCode(record),
                labName = record.labName,
                labTypeName = string.IsNullOrEmpty(record.labTypeName) ? currentSceneLabType : record.labTypeName,
                campus = record.campus,
                building = record.building,
                floor = record.floor,
                roomNo = record.roomNo,
                capacity = record.capacity,
                areaM2 = record.areaM2,
                dutyUserName = string.IsNullOrEmpty(record.dutyUserName) ? "未指定" : record.dutyUserName,
                runningStatus = NormalizeRunningStatus(record),
                equipment = string.IsNullOrEmpty(record.description) ? "已接入后台台账，暂未配置设备摘要" : record.description,
                description = string.IsNullOrEmpty(record.description) ? "已从后台同步实验室基础信息。" : record.description
            });
        }
        return labs;
    }

    private static string ResolveLabCode(LabApiRecord record)
    {
        if (!string.IsNullOrEmpty(record.roomNo))
            return $"LAB-{record.id:D4}-{record.roomNo}";

        return $"LAB-{record.id:D4}";
    }

    private static int NormalizeRunningStatus(LabApiRecord record)
    {
        if (record.runningStatus >= 1 && record.runningStatus <= 3)
            return record.runningStatus;

        if (record.openStatus == 1)
            return 1;

        if (record.openStatus == 2)
            return 3;

        return 2;
    }

    private static bool TryParseTimeSlot(string timeSlot, out string startTime, out string endTime)
    {
        startTime = string.Empty;
        endTime = string.Empty;
        if (string.IsNullOrEmpty(timeSlot))
            return false;

        string normalized = timeSlot.Replace("—", "-").Replace("－", "-");
        string[] parts = normalized.Split('-');
        if (parts.Length != 2)
            return false;

        startTime = parts[0].Trim();
        endTime = parts[1].Trim();
        return !string.IsNullOrEmpty(startTime) && !string.IsNullOrEmpty(endTime);
    }

    private static string BuildReservationPurpose(LabBorrowRequest request)
    {
        string purpose = string.IsNullOrEmpty(request.purpose) ? "Unity 3D 场景预约" : request.purpose.Trim();
        return $"{purpose}；申请人：{request.applicantName}；使用人数：{request.peopleCount}";
    }

    private void NotifyPanelsRefreshed()
    {
        LabInfoPanelUI[] panels = FindObjectsOfType<LabInfoPanelUI>(true);
        for (int i = 0; i < panels.Length; i++)
        {
            panels[i].RefreshData();
        }
    }

    private System.Collections.IEnumerator SimulateApiCall(LabBorrowRequest request, Action<bool, string> callback)
    {
        yield return new WaitForSeconds(0.8f);

        // 简单校验
        if (string.IsNullOrWhiteSpace(request.applicantName))
        {
            callback?.Invoke(false, "请填写申请人姓名");
            yield break;
        }
        if (request.labId <= 0)
        {
            callback?.Invoke(false, "请选择目标实验室");
            yield break;
        }
        if (request.peopleCount <= 0)
        {
            callback?.Invoke(false, "使用人数必须大于 0");
            yield break;
        }

        WebGLBridge.EmitWebEvent(
            "scene.lab_reservation_submitted",
            $"已提交实验室预约：{request.reserveDate} {request.timeSlot}",
            "success",
            null,
            request.labId.ToString());
        callback?.Invoke(true, "预约申请已提交，请等待管理员审核！");
    }

    /// <summary>
    /// 公开方法：外部强制生成 Mock 数据（用于编辑器模式测试）
    /// </summary>
    public void GenerateMockDataPublic()
    {
        GenerateMockData();
    }

    /// <summary>
    /// 自动生成 Mock 数据（涵盖各类型实验室）</summary>
    private void GenerateMockData()
    {
        // 兜底数据 — Class_Lab→教学实验室; Common_Lab→化学/物理/生物; Computer→计算机; Discussion→小组讨论室
        mockLabData = new List<LabInfoData>
        {
            // === Class_Lab: 教学实验室 ===
            new LabInfoData { id = 1, labCode = "LAB-MOCK-001", labName = "A301 教学实验室",
                labTypeName = "教学实验室", campus = "东校区", building = "教学楼A",
                floor = 3, roomNo = "A301", capacity = 40, areaM2 = 85f,
                dutyUserName = "王管理员", runningStatus = 1,
                equipment = "多功能实验台×20、投影仪×1、白板×2",
                description = "标准教学实验环境（离线兜底数据）。" },
            new LabInfoData { id = 2, labCode = "LAB-MOCK-002", labName = "A302 教学实验室",
                labTypeName = "教学实验室", campus = "东校区", building = "教学楼A",
                floor = 3, roomNo = "A302", capacity = 36, areaM2 = 78f,
                dutyUserName = "王管理员", runningStatus = 1,
                equipment = "多功能实验台×18、投影仪×1",
                description = "教学实验室（离线兜底数据）。" },
            // === Common_Lab: 化学/物理/生物实验室 ===
            new LabInfoData { id = 3, labCode = "LAB-MOCK-003", labName = "A201 化学实验室",
                labTypeName = "化学实验室", campus = "东校区", building = "化学楼",
                floor = 2, roomNo = "A201", capacity = 30, areaM2 = 90f,
                dutyUserName = "王管理员", runningStatus = 1,
                equipment = "通风橱×4、实验台×15",
                description = "标准化学实验室（离线兜底数据）。" },
            new LabInfoData { id = 4, labCode = "LAB-MOCK-004", labName = "B103 物理实验室",
                labTypeName = "物理实验室", campus = "东校区", building = "理工楼",
                floor = 1, roomNo = "B103", capacity = 40, areaM2 = 120f,
                dutyUserName = "王管理员", runningStatus = 1,
                equipment = "物理实验台×20、示波器×10",
                description = "大学物理实验室（离线兜底数据）。" },
            new LabInfoData { id = 5, labCode = "LAB-MOCK-005", labName = "D108 生物培养实验室",
                labTypeName = "生物实验室", campus = "东校区", building = "生科楼",
                floor = 1, roomNo = "D108", capacity = 25, areaM2 = 80f,
                dutyUserName = "王管理员", runningStatus = 1,
                equipment = "培养箱×10、显微镜×15",
                description = "生物培养实验室（离线兜底数据）。" },
            // === Computer_Lab: 计算机实验室 ===
            new LabInfoData { id = 6, labCode = "LAB-MOCK-006", labName = "C202 计算机实验室",
                labTypeName = "计算机实验室", campus = "东校区", building = "信息楼",
                floor = 2, roomNo = "C202", capacity = 50, areaM2 = 100f,
                dutyUserName = "李老师", runningStatus = 1,
                equipment = "高性能工作站×50",
                description = "计算机实验室（离线兜底数据）。" },
            // === Discussion_Lab: 小组讨论室（数据库暂无，单独兜底） ===
            new LabInfoData { id = 7, labCode = "LAB-MOCK-007", labName = "D101 小组讨论室",
                labTypeName = "小组讨论室", campus = "东校区", building = "教学楼D",
                floor = 1, roomNo = "D101", capacity = 12, areaM2 = 28f,
                dutyUserName = "行政管理员", runningStatus = 1,
                equipment = "会议桌×1、显示屏×1、白板×2",
                description = "小组讨论室（离线兜底数据）。" },
        };
    }
}

[Serializable]
public class LabPageApiResponse
{
    public int code;
    public string message;
    public LabPageApiData data;
}

[Serializable]
public class LabPageApiData
{
    public List<LabApiRecord> records;
    public int total;
    public int pageNum;
    public int pageSize;
    public int pages;
}

[Serializable]
public class LabApiRecord
{
    public int id;
    public string labName;
    public int labTypeId;
    public string labTypeName;
    public string campus;
    public string building;
    public int floor;
    public string roomNo;
    public int capacity;
    public float areaM2;
    public int dutyUserId;
    public string dutyUserName;
    public int runningStatus;
    public string runningStatusDesc;
    public int openStatus;
    public string openStatusDesc;
    public string description;
}

[Serializable]
public class LabDetailApiResponse
{
    public int code;
    public string message;
    public LabDetailApiRecord data;
}

[Serializable]
public class LabDetailApiRecord : LabApiRecord
{
    public List<LabScheduleApiRecord> weeklySchedules;
}

[Serializable]
public class LabScheduleApiRecord
{
    public int id;
    public int dayOfWeek;
    public string openTime;
    public string closeTime;
}

[Serializable]
public class LabReservationCreateApiRequest
{
    public int labId;
    public string reservationDate;
    public string startTime;
    public string endTime;
    public string purpose;
    public string courseName;
}
