using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

/// <summary>
/// 试剂器材室数据提供者（单例）
/// 初期使用 Mock 数据，后续可替换为 API 调用
/// </summary>
public class ReagentDataProvider : MonoBehaviour
{
    public static ReagentDataProvider Instance { get; private set; }

    [Header("Mock 数据")]
    public List<ReagentData> mockReagents = new List<ReagentData>();
    public List<ReagentPickupRecord> mockPickupRecords = new List<ReagentPickupRecord>();
    public List<DeviceData> mockDevices = new List<DeviceData>();
    public LabIoTData mockIoTData = new LabIoTData();
    public List<AlertRecord> mockAlerts = new List<AlertRecord>();
    public List<DeviceBorrowRecord> mockBorrowRecords = new List<DeviceBorrowRecord>();
    public List<DeviceRepairRecord> mockRepairRecords = new List<DeviceRepairRecord>();

    private readonly Dictionary<int, string> labNameMap = new Dictionary<int, string>();
    private Coroutine backendSyncCoroutine;

    private void Awake()
    {
        if (Instance == null) Instance = this;

        if (mockReagents == null || mockReagents.Count == 0) GenerateMockReagents();
        if (mockPickupRecords == null || mockPickupRecords.Count == 0) GenerateMockPickupRecords();
        if (mockDevices == null || mockDevices.Count == 0) GenerateMockDevices();
        if (mockAlerts == null || mockAlerts.Count == 0) GenerateMockAlerts();
        if (mockBorrowRecords == null || mockBorrowRecords.Count == 0) GenerateMockBorrowRecords();
        if (mockRepairRecords == null || mockRepairRecords.Count == 0) GenerateMockRepairRecords();
        GenerateMockIoT();
        AttachSensorDataToDevices();
        UpdateIoTStatus();
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

    // ─────────────────────────────────────────────────────────
    //  公共数据访问
    // ─────────────────────────────────────────────────────────

    public List<ReagentData> GetAllReagents() => mockReagents;
    public ReagentData GetReagentById(int reagentId) => mockReagents.Find(r => r.id == reagentId);
    public List<ReagentPickupRecord> GetPickupRecords() => mockPickupRecords;

    public List<ReagentData> GetWarningReagents()
    {
        var result = new List<ReagentData>();
        foreach (var r in mockReagents)
        {
            if (r.IsLowStock || r.IsExpired || r.IsExpiringSoon)
                result.Add(r);
        }
        return result;
    }

    public List<DeviceData> GetAllDevices() => mockDevices;
    public DeviceData GetDeviceById(int deviceId) => mockDevices.Find(d => d.id == deviceId);

    public List<DeviceData> GetDevicesByStatus(string status)
    {
        var result = new List<DeviceData>();
        foreach (var d in mockDevices)
        {
            if (d.deviceStatus == status)
                result.Add(d);
        }
        return result;
    }

    public LabIoTData GetIoTData() => mockIoTData;
    public List<AlertRecord> GetAlerts() => mockAlerts;
    public List<DeviceBorrowRecord> GetBorrowRecords() => mockBorrowRecords;
    public List<DeviceRepairRecord> GetRepairRecords() => mockRepairRecords;

    public void SubmitPickupRequest(ReagentPickupRequest request, Action<bool, string> callback)
    {
        if (WebGLBridge.HasUsableContext)
        {
            StartCoroutine(SubmitPickupRequestToBackend(request, callback));
            return;
        }

        bool success = SubmitPickupRequestLocal(request, out string message);
        callback?.Invoke(success, message);
    }

    public void SubmitDeviceBorrow(DeviceBorrowRequest request, Action<bool, string> callback)
    {
        if (WebGLBridge.HasUsableContext)
        {
            StartCoroutine(SubmitDeviceBorrowToBackend(request, callback));
            return;
        }

        bool success = SubmitDeviceBorrowLocal(request, out string message);
        callback?.Invoke(success, message);
    }

    public void SubmitRepairRequest(int deviceId, string description, Action<bool, string> callback)
    {
        if (WebGLBridge.HasUsableContext)
        {
            StartCoroutine(SubmitRepairRequestToBackend(deviceId, description, callback));
            return;
        }

        bool success = SubmitRepairRequestLocal(deviceId, description, out string message);
        callback?.Invoke(success, message);
    }

    public void AdvanceMockTelemetry()
    {
        // 始终在本地随机化设备级传感器数据（保证设备实时状态表持续变化）
        RandomizePerDeviceSensorData();

        if (WebGLBridge.HasUsableContext)
        {
            StartCoroutine(AdvanceTelemetryViaBackend());
            return;
        }

        // 本地模式：同时随机化全局 IoT 数据
        mockIoTData.temperature = Mathf.Clamp(mockIoTData.temperature + UnityEngine.Random.Range(-0.3f, 0.3f), 21f, 34f);
        mockIoTData.humidity = Mathf.Clamp(mockIoTData.humidity + UnityEngine.Random.Range(-0.6f, 0.6f), 38f, 66f);
        mockIoTData.smoke = Mathf.Clamp(mockIoTData.smoke + UnityEngine.Random.Range(-0.003f, 0.003f), 0.01f, 0.08f);
        mockIoTData.gas = Mathf.Clamp(mockIoTData.gas + UnityEngine.Random.Range(-0.002f, 0.002f), 0.005f, 0.06f);
        mockIoTData.battery = Mathf.Clamp(mockIoTData.battery + UnityEngine.Random.Range(-0.5f, 0.2f), 48f, 96f);
        mockIoTData.signalStrength = Mathf.Clamp(mockIoTData.signalStrength + UnityEngine.Random.Range(-1.5f, 1.5f), 42f, 95f);
        mockIoTData.lastUpdateStr = DateTime.Now.ToString("HH:mm:ss");

        UpdateIoTStatus();
    }

    /// <summary>
    /// 随机化每个设备的传感器数据（温度、湿度、电量），保证设备实时状态表持续变化
    /// </summary>
    private void RandomizePerDeviceSensorData()
    {
        string now = DateTime.Now.ToString("HH:mm:ss");
        foreach (DeviceData device in mockDevices)
        {
            if (device.sensorData == null)
                continue;

            if (device.sensorData.temperature >= 0f)
                device.sensorData.temperature = Mathf.Clamp(device.sensorData.temperature + UnityEngine.Random.Range(-0.25f, 0.25f), 20f, 55f);

            if (device.sensorData.humidity >= 0f)
                device.sensorData.humidity = Mathf.Clamp(device.sensorData.humidity + UnityEngine.Random.Range(-0.4f, 0.4f), 35f, 70f);

            if (device.sensorData.battery >= 0f)
                device.sensorData.battery = Mathf.Clamp(device.sensorData.battery + UnityEngine.Random.Range(-0.4f, 0.2f), 15f, 98f);

            device.sensorData.lastReportStr = now;
            device.sensorData.status = ResolveDeviceSensorStatus(device.sensorData);
        }
    }

    private bool SubmitPickupRequestLocal(ReagentPickupRequest request, out string message)
    {
        message = "提交失败";
        ReagentData reagent = GetReagentById(request.reagentId);
        if (reagent == null)
        {
            message = "未找到对应试剂";
            return false;
        }

        if (request.quantity <= 0f)
        {
            message = "领用数量必须大于 0";
            return false;
        }

        if (request.quantity > reagent.currentStock)
        {
            message = $"库存不足，当前仅剩 {reagent.currentStock}{reagent.unit}";
            return false;
        }

        reagent.currentStock = Mathf.Max(0f, reagent.currentStock - request.quantity);
        ConsumeBatchStock(reagent, request.quantity);

        mockPickupRecords.Insert(0, new ReagentPickupRecord
        {
            id = mockPickupRecords.Count + 1,
            reagentId = reagent.id,
            reagentName = reagent.name,
            quantity = request.quantity,
            unit = request.unit,
            applicantName = request.applicantName,
            pickupTimeStr = string.IsNullOrEmpty(request.pickupDateStr) ? DateTime.Now.ToString("yyyy-MM-dd HH:mm") : request.pickupDateStr,
            purpose = request.purpose,
            status = "已领用"
        });

        if (reagent.IsLowStock)
        {
            mockAlerts.Insert(0, new AlertRecord
            {
                timeStr = DateTime.Now.ToString("HH:mm"),
                deviceName = reagent.name,
                message = $"库存低于安全线，当前剩余 {reagent.currentStock}{reagent.unit}（安全线 {reagent.safetyStock}{reagent.unit}）",
                level = "warning"
            });
        }

        message = $"{reagent.name} 领用申请已提交并登记完成";
        WebGLBridge.EmitWebEvent(
            "scene.material_receive_submitted",
            $"已提交 {reagent.name} 领用申请，数量 {request.quantity}{request.unit}",
            "success",
            reagent.name,
            reagent.id.ToString());
        return true;
    }

    private bool SubmitDeviceBorrowLocal(DeviceBorrowRequest request, out string message)
    {
        message = "提交失败";
        DeviceData device = GetDeviceById(request.deviceId);
        if (device == null)
        {
            message = "未找到对应设备";
            return false;
        }

        if (device.deviceStatus != "闲置")
        {
            message = $"设备当前状态为「{device.deviceStatus}」，无法借用";
            return false;
        }

        device.deviceStatus = "借用中";
        mockBorrowRecords.Insert(0, new DeviceBorrowRecord
        {
            id = mockBorrowRecords.Count + 1,
            deviceName = device.name,
            borrowerName = request.applicantName,
            actualOutTimeStr = string.IsNullOrEmpty(request.expectedStartStr) ? DateTime.Now.ToString("yyyy-MM-dd HH:mm") : request.expectedStartStr,
            actualReturnTimeStr = request.expectedReturnStr,
            status = 1
        });

        message = $"设备「{device.name}」借用申请已提交";
        WebGLBridge.EmitWebEvent(
            "scene.device_borrow_submitted",
            $"已提交设备借用：{device.name}",
            "success",
            device.name,
            device.id.ToString());
        return true;
    }

    private bool SubmitRepairRequestLocal(int deviceId, string description, out string message)
    {
        message = "提交失败";
        DeviceData device = GetDeviceById(deviceId);
        if (device == null)
        {
            message = "未找到对应设备";
            return false;
        }

        if (device.deviceStatus == "报废")
        {
            message = "该设备已报废，不能再发起维修";
            return false;
        }

        device.deviceStatus = "维修中";
        device.healthScore = Mathf.Min(device.healthScore, 58);

        mockRepairRecords.Insert(0, new DeviceRepairRecord
        {
            id = mockRepairRecords.Count + 1,
            deviceId = device.id,
            deviceName = device.name,
            reportTimeStr = DateTime.Now.ToString("yyyy-MM-dd HH:mm"),
            description = description,
            handlerName = "设备管理员",
            repairStatus = "待处理"
        });

        mockAlerts.Insert(0, new AlertRecord
        {
            timeStr = DateTime.Now.ToString("HH:mm"),
            deviceName = device.name,
            message = $"已提交维修上报：{description}",
            level = "warning"
        });

        message = $"设备「{device.name}」已进入报修流程";
        WebGLBridge.EmitWebEvent(
            "scene.device_repair_submitted",
            $"已上报设备维修：{device.name}",
            "warning",
            device.name,
            device.id.ToString());
        return true;
    }

    private void HandleWebContextChanged(SmartLabWebContext _)
    {
        TryLoadFromBackend();
    }

    private void HandleRealtimeEvent(SmartLabRealtimeMessage message)
    {
        if (message == null || string.IsNullOrEmpty(message.eventName))
            return;

        if (message.eventName.StartsWith("material.", StringComparison.OrdinalIgnoreCase) ||
            message.eventName.StartsWith("equipment.", StringComparison.OrdinalIgnoreCase) ||
            message.eventName.StartsWith("sensor.", StringComparison.OrdinalIgnoreCase))
        {
            ScheduleBackendReload();
        }
    }

    private void TryLoadFromBackend()
    {
        if (!WebGLBridge.HasUsableContext)
            return;

        StartBackendSync();
    }

    public void ForceBackendRefresh()
    {
        if (WebGLBridge.HasUsableContext)
        {
            StartBackendSync();
            return;
        }

        UpdateDerivedMonitoringData();
        NotifyPanelsRefreshed();
    }

    private void ScheduleBackendReload(float delaySeconds = 0.35f)
    {
        CancelInvoke(nameof(ReloadFromBackend));
        Invoke(nameof(ReloadFromBackend), delaySeconds);
    }

    private void ReloadFromBackend()
    {
        StartBackendSync();
    }

    private void StartBackendSync()
    {
        if (!isActiveAndEnabled)
            return;

        if (backendSyncCoroutine != null)
        {
            StopCoroutine(backendSyncCoroutine);
        }

        backendSyncCoroutine = StartCoroutine(SyncBackendData());
    }

    private IEnumerator SyncBackendData()
    {
        yield return LoadLabNames();
        yield return LoadMaterials();
        yield return LoadPickupRecords();
        yield return LoadDevices();
        yield return LoadBorrowRecords();
        yield return LoadRepairRecords();
        yield return LoadSensorSummaryFromBackend();

        UpdateDerivedMonitoringData();
        NotifyPanelsRefreshed();
        WebGLBridge.EmitWebEvent(
            "scene.resource_data_synced",
            $"已同步 {mockReagents.Count} 条试剂和 {mockDevices.Count} 台设备数据",
            "success");
        backendSyncCoroutine = null;
    }

    private IEnumerator LoadLabNames()
    {
        string responseText = null;
        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbGET, "lab-management/labs?pageNum=1&pageSize=100"),
            text => responseText = text,
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage) || string.IsNullOrEmpty(responseText))
        {
            Debug.LogWarning($"[ReagentDataProvider] 拉取实验室映射失败: {errorMessage}");
            yield break;
        }

        LabPageApiResponse response = null;
        try
        {
            response = JsonUtility.FromJson<LabPageApiResponse>(responseText);
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[ReagentDataProvider] 解析实验室映射失败: {ex.Message}");
            yield break;
        }

        labNameMap.Clear();
        if (response == null || response.data == null || response.data.records == null)
            yield break;

        for (int i = 0; i < response.data.records.Count; i++)
        {
            LabApiRecord record = response.data.records[i];
            labNameMap[record.id] = record.labName;
        }
    }

    private IEnumerator LoadMaterials()
    {
        string responseText = null;
        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbGET, "material/materials?pageNum=1&pageSize=100"),
            text => responseText = text,
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage) || string.IsNullOrEmpty(responseText))
        {
            Debug.LogWarning($"[ReagentDataProvider] 拉取物资列表失败: {errorMessage}");
            yield break;
        }

        MaterialPageApiResponse response = null;
        try
        {
            response = JsonUtility.FromJson<MaterialPageApiResponse>(responseText);
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[ReagentDataProvider] 解析物资列表失败: {ex.Message}");
            yield break;
        }

        if (response == null || response.data == null || response.data.records == null)
            yield break;

        List<ReagentData> reagents = new List<ReagentData>();
        for (int i = 0; i < response.data.records.Count; i++)
        {
            MaterialApiRecord material = response.data.records[i];
            MaterialDetailApiRecord detail = null;
            yield return LoadMaterialDetail(material.id, loadedDetail => detail = loadedDetail);
            reagents.Add(MapMaterialToReagent(material, detail));
        }

        mockReagents = reagents;
    }

    private IEnumerator LoadMaterialDetail(int materialId, Action<MaterialDetailApiRecord> onLoaded)
    {
        string responseText = null;
        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbGET, $"material/materials/{materialId}"),
            text => responseText = text,
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage) || string.IsNullOrEmpty(responseText))
        {
            Debug.LogWarning($"[ReagentDataProvider] 拉取物资详情失败 materialId={materialId}: {errorMessage}");
            onLoaded?.Invoke(null);
            yield break;
        }

        try
        {
            MaterialDetailApiResponse response = JsonUtility.FromJson<MaterialDetailApiResponse>(responseText);
            onLoaded?.Invoke(response != null ? response.data : null);
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[ReagentDataProvider] 解析物资详情失败 materialId={materialId}: {ex.Message}");
            onLoaded?.Invoke(null);
        }
    }

    private IEnumerator LoadPickupRecords()
    {
        string responseText = null;
        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbGET, "material/receives?pageNum=1&pageSize=50"),
            text => responseText = text,
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage) || string.IsNullOrEmpty(responseText))
        {
            Debug.LogWarning($"[ReagentDataProvider] 拉取领用记录失败: {errorMessage}");
            yield break;
        }

        try
        {
            ReceivePageApiResponse response = JsonUtility.FromJson<ReceivePageApiResponse>(responseText);
            mockPickupRecords = MapPickupRecords(response != null && response.data != null ? response.data.records : null);
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[ReagentDataProvider] 解析领用记录失败: {ex.Message}");
        }
    }

    private IEnumerator LoadDevices()
    {
        string responseText = null;
        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbGET, "equipment/devices?pageNum=1&pageSize=100"),
            text => responseText = text,
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage) || string.IsNullOrEmpty(responseText))
        {
            Debug.LogWarning($"[ReagentDataProvider] 拉取设备列表失败: {errorMessage}");
            yield break;
        }

        try
        {
            DevicePageApiResponse response = JsonUtility.FromJson<DevicePageApiResponse>(responseText);
            mockDevices = MapDevices(response != null && response.data != null ? response.data.records : null);
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[ReagentDataProvider] 解析设备列表失败: {ex.Message}");
        }
    }

    private IEnumerator LoadBorrowRecords()
    {
        string responseText = null;
        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbGET, "equipment/borrow-applies/my?pageNum=1&pageSize=50"),
            text => responseText = text,
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage) || string.IsNullOrEmpty(responseText))
        {
            Debug.LogWarning($"[ReagentDataProvider] 拉取设备借用记录失败: {errorMessage}");
            yield break;
        }

        try
        {
            BorrowApplyPageApiResponse response = JsonUtility.FromJson<BorrowApplyPageApiResponse>(responseText);
            mockBorrowRecords = MapBorrowRecords(response != null && response.data != null ? response.data.records : null);
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[ReagentDataProvider] 解析设备借用记录失败: {ex.Message}");
        }
    }

    private IEnumerator LoadRepairRecords()
    {
        string responseText = null;
        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbGET, "equipment/repairs?pageNum=1&pageSize=50"),
            text => responseText = text,
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage) || string.IsNullOrEmpty(responseText))
        {
            Debug.LogWarning($"[ReagentDataProvider] 拉取维修记录失败: {errorMessage}");
            yield break;
        }

        try
        {
            RepairPageApiResponse response = JsonUtility.FromJson<RepairPageApiResponse>(responseText);
            mockRepairRecords = MapRepairRecords(response != null && response.data != null ? response.data.records : null);
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[ReagentDataProvider] 解析维修记录失败: {ex.Message}");
        }
    }

    private string lastPostedSensorNo = "";

    private IEnumerator AdvanceTelemetryViaBackend()
    {
        int labId = ResolveCurrentLabId();
        if (labId <= 0)
        {
            yield return LoadSensorSummaryFromBackend();
            yield break;
        }

        // 先获取当前传感器列表，确定上报哪个传感器
        string listText = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbGET,
                $"iot/labs/{labId}/summary"),
            text => listText = text,
            _ => { });

        string sensorNo = ResolveSensorNoFromResponse(listText, labId);
        if (string.IsNullOrEmpty(sensorNo))
        {
            yield return LoadSensorSummaryFromBackend();
            yield break;
        }

        // 生成小幅随机波动的传感器数据
        float temp = mockIoTData.temperature > 0 ? mockIoTData.temperature : 23.5f;
        float hum = mockIoTData.humidity > 0 ? mockIoTData.humidity : 48f;
        float newTemp = Mathf.Clamp(temp + UnityEngine.Random.Range(-0.8f, 0.8f), 18f, 40f);
        float newHum = Mathf.Clamp(hum + UnityEngine.Random.Range(-2f, 2f), 30f, 75f);

        var payload = new SensorDataPostRequest
        {
            sensorNo = sensorNo,
            detectedAt = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
            detectedData = new SensorDetectedData
            {
                temperature = Mathf.Round(newTemp * 10f) / 10f,
                humidity = Mathf.Round(newHum * 10f) / 10f,
            }
        };

        lastPostedSensorNo = sensorNo;

        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbPOST,
                "iot/sensor-data",
                JsonUtility.ToJson(payload)),
            _ => { },
            error => Debug.Log($"[ReagentDataProvider] 传感器上报失败: {error}"));

        // 上报后立即读取最新汇总
        yield return LoadSensorSummaryFromBackend();
    }

    private string ResolveSensorNoFromResponse(string responseText, int labId)
    {
        if (string.IsNullOrEmpty(responseText)) return "";
        try
        {
            LabIotSummaryApiResponse apiResponse = JsonUtility.FromJson<LabIotSummaryApiResponse>(responseText);
            if (apiResponse?.data?.sensors != null && apiResponse.data.sensors.Length > 0)
            {
                return apiResponse.data.sensors[0].sensorNo;
            }
        }
        catch { }
        // 根据 labId 回退到已知传感器映射
        return labId switch { 1 => "SEN-001", 2 => "SEN-002", 3 => "SEN-003", 4 => "SEN-004", _ => "" };
    }

    private IEnumerator LoadSensorSummaryFromBackend()
    {
        int labId = ResolveCurrentLabId();
        if (labId <= 0)
        {
            UpdateDerivedMonitoringData();
            yield break;
        }

        string responseText = null;
        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbGET,
                $"iot/labs/{labId}/summary"),
            text => responseText = text,
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage) || string.IsNullOrEmpty(responseText))
        {
            Debug.Log($"[ReagentDataProvider] 传感器汇总接口不可用，使用本地推导数据: {errorMessage}");
            UpdateDerivedMonitoringData();
            yield break;
        }

        try
        {
            LabIotSummaryApiResponse apiResponse = JsonUtility.FromJson<LabIotSummaryApiResponse>(responseText);
            LabIotSummaryResponse summary = apiResponse != null ? apiResponse.data : null;
            if (summary == null || string.IsNullOrEmpty(summary.status) || summary.status == "no_sensor" || summary.status == "no_data")
            {
                Debug.Log($"[ReagentDataProvider] 传感器汇总无有效数据 (status={summary?.status ?? "null"})，使用本地推导");
                UpdateDerivedMonitoringData();
                yield break;
            }

            mockIoTData.temperature = summary.temperature > 0 ? summary.temperature : mockIoTData.temperature;
            mockIoTData.humidity = summary.humidity > 0 ? summary.humidity : mockIoTData.humidity;
            mockIoTData.smoke = summary.smoke > 0 ? summary.smoke : mockIoTData.smoke;
            mockIoTData.gas = summary.gas > 0 ? summary.gas : mockIoTData.gas;
            mockIoTData.battery = summary.battery > 0 ? summary.battery : mockIoTData.battery;
            mockIoTData.signalStrength = summary.signalStrength > 0 ? summary.signalStrength : mockIoTData.signalStrength;            mockIoTData.status = summary.status ?? "normal";
            if (!string.IsNullOrEmpty(summary.lastUpdateStr))
            {
                mockIoTData.lastUpdateStr = summary.lastUpdateStr;
            }

            Debug.Log($"[ReagentDataProvider] 已从后端同步实验室 IoT 数据: temp={mockIoTData.temperature:F1}°C, humidity={mockIoTData.humidity:F0}%, status={mockIoTData.status}");
            NotifyPanelsRefreshed();
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[ReagentDataProvider] 解析传感器汇总失败: {ex.Message}");
            UpdateDerivedMonitoringData();
        }
    }

    private int ResolveCurrentLabId()
    {
        for (int i = 0; i < mockDevices.Count; i++)
        {
            if (mockDevices[i].labId > 0)
                return mockDevices[i].labId;
        }
        if (mockReagents.Count > 0 && mockReagents[0].id > 0)
            return mockReagents[0].id;
        return 0;
    }

    private IEnumerator SubmitPickupRequestToBackend(ReagentPickupRequest request, Action<bool, string> callback)
    {
        if (request == null)
        {
            callback?.Invoke(false, "领用请求不能为空");
            yield break;
        }

        int quantity = Mathf.RoundToInt(request.quantity);
        if (Mathf.Abs(request.quantity - quantity) > 0.001f)
        {
            callback?.Invoke(false, "当前后端领用接口仅支持整数数量");
            yield break;
        }

        MaterialReceiveApplyApiRequest payload = new MaterialReceiveApplyApiRequest
        {
            materialId = request.reagentId,
            expectedQuantity = quantity,
            purpose = request.purpose
        };

        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbPOST,
                "material/receives/apply",
                JsonUtility.ToJson(payload)),
            _ => { },
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage))
        {
            callback?.Invoke(false, errorMessage);
            yield break;
        }

        yield return SyncBackendData();
        WebGLBridge.EmitWebEvent(
            "scene.material_receive_submitted",
            $"已提交 {request.reagentName} 领用申请，数量 {quantity}{request.unit}",
            "success",
            request.reagentName,
            request.reagentId.ToString());
        callback?.Invoke(true, $"{request.reagentName} 领用申请已提交，当前状态为待审核");
    }

    private IEnumerator SubmitDeviceBorrowToBackend(DeviceBorrowRequest request, Action<bool, string> callback)
    {
        if (request == null)
        {
            callback?.Invoke(false, "借用请求不能为空");
            yield break;
        }

        DeviceBorrowApplyApiRequest payload = new DeviceBorrowApplyApiRequest
        {
            expectedStartTime = NormalizeDateTime(request.expectedStartStr, "09:00:00"),
            expectedReturnTime = NormalizeDateTime(request.expectedReturnStr, "18:00:00"),
            purpose = request.purpose
        };

        if (string.IsNullOrEmpty(payload.expectedStartTime) || string.IsNullOrEmpty(payload.expectedReturnTime))
        {
            callback?.Invoke(false, "请填写有效的借用开始时间和归还时间，格式示例：2026-07-10 14:00");
            yield break;
        }

        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbPOST,
                $"equipment/devices/{request.deviceId}/borrow",
                JsonUtility.ToJson(payload)),
            _ => { },
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage))
        {
            callback?.Invoke(false, errorMessage);
            yield break;
        }

        yield return SyncBackendData();
        WebGLBridge.EmitWebEvent(
            "scene.device_borrow_submitted",
            $"已提交设备借用：{request.deviceName}",
            "success",
            request.deviceName,
            request.deviceId.ToString());
        callback?.Invoke(true, $"设备「{request.deviceName}」借用申请已提交，待管理员审批出库");
    }

    private IEnumerator SubmitRepairRequestToBackend(int deviceId, string description, Action<bool, string> callback)
    {
        DeviceRepairReportApiRequest payload = new DeviceRepairReportApiRequest
        {
            deviceId = deviceId,
            faultDesc = description
        };

        string errorMessage = null;
        yield return SmartLabWebApi.SendRequest(
            SmartLabWebApi.CreateRequest(UnityEngine.Networking.UnityWebRequest.kHttpVerbPOST,
                "equipment/repairs",
                JsonUtility.ToJson(payload)),
            _ => { },
            error => errorMessage = error);

        if (!string.IsNullOrEmpty(errorMessage))
        {
            callback?.Invoke(false, errorMessage);
            yield break;
        }

        yield return SyncBackendData();
        WebGLBridge.EmitWebEvent(
            "scene.device_repair_submitted",
            "已提交设备报修，等待管理员处理",
            "warning",
            null,
            deviceId.ToString());
        callback?.Invoke(true, "设备报修已提交，等待管理员处理");
    }

    private ReagentData MapMaterialToReagent(MaterialApiRecord material, MaterialDetailApiRecord detail)
    {
        string materialName = detail != null && !string.IsNullOrEmpty(detail.materialName) ? detail.materialName : material.materialName;
        string category = ResolveMaterialCategory(materialName);
        return new ReagentData
        {
            id = material.id,
            name = materialName,
            casNo = string.IsNullOrEmpty(material.materialNo) ? $"MAT-{material.id:D4}" : material.materialNo,
            spec = BuildReagentSpec(material, detail),
            unit = string.IsNullOrEmpty(material.unit) ? "件" : material.unit,
            category = category,
            currentStock = material.currentStock,
            safetyStock = material.safetyStock,
            location = ResolveLabName(material.labId),
            expireDateStr = string.IsNullOrEmpty(material.earliestExpiryDate) ? "2099-12-31" : material.earliestExpiryDate,
            safetyLevel = ResolveSafetyLevel(materialName, category),
            batches = MapReagentBatches(material.id, detail != null ? detail.batches : null)
        };
    }

    private List<ReagentBatchData> MapReagentBatches(int reagentId, List<MaterialBatchApiRecord> batches)
    {
        List<ReagentBatchData> result = new List<ReagentBatchData>();
        if (batches == null)
            return result;

        for (int i = 0; i < batches.Count; i++)
        {
            MaterialBatchApiRecord batch = batches[i];
            result.Add(new ReagentBatchData
            {
                id = batch.id,
                reagentId = reagentId,
                batchNo = batch.batchNo,
                entryDateStr = FormatDateLabel(batch.stockInTime, "—"),
                entryQuantity = batch.quantity,
                remainQuantity = batch.remaining,
                status = string.IsNullOrEmpty(batch.batchStatusDesc) ? "正常" : batch.batchStatusDesc,
            });
        }

        return result;
    }

    private List<ReagentPickupRecord> MapPickupRecords(List<ReceiveApplyApiRecord> records)
    {
        List<ReagentPickupRecord> result = new List<ReagentPickupRecord>();
        if (records == null)
            return result;

        for (int i = 0; i < records.Count; i++)
        {
            ReceiveApplyApiRecord record = records[i];
            ReagentData reagent = GetReagentById(record.materialId);
            result.Add(new ReagentPickupRecord
            {
                id = record.id,
                reagentId = record.materialId,
                reagentName = record.materialName,
                quantity = record.expectedQuantity,
                unit = reagent != null ? reagent.unit : "件",
                applicantName = record.applicantId > 0 ? $"用户#{record.applicantId}" : "当前用户",
                pickupTimeStr = FormatDateLabel(record.createdAt, "—"),
                purpose = string.IsNullOrEmpty(record.purpose) ? "未填写" : record.purpose,
                status = ResolvePickupStatus(record.applyStatus, record.applyStatusDesc)
            });
        }

        return result;
    }

    private List<DeviceData> MapDevices(List<DeviceApiRecord> records)
    {
        List<DeviceData> result = new List<DeviceData>();
        if (records == null)
            return result;

        for (int i = 0; i < records.Count; i++)
        {
            DeviceApiRecord record = records[i];
            result.Add(new DeviceData
            {
                id = record.id,
                deviceNo = record.deviceNo,
                name = record.name,
                spec = BuildDeviceSpec(record),
                category = string.IsNullOrEmpty(record.deviceTypeName) ? "设备" : record.deviceTypeName,
                labId = record.labId,
                labName = ResolveLabName(record.labId),
                purchaseDateStr = FormatDateLabel(record.purchaseDate, "—"),
                purchasePrice = record.purchasePrice,
                expireDateStr = FormatDateLabel(record.expireDate, "—"),
                deviceStatus = ResolveDeviceStatus(record.deviceStatus, record.deviceStatusDesc),
                healthScore = record.healthScore <= 0 ? 60 : record.healthScore,
                remark = record.remark,
            });
        }

        return result;
    }

    private List<DeviceBorrowRecord> MapBorrowRecords(List<BorrowApplyApiRecord> records)
    {
        List<DeviceBorrowRecord> result = new List<DeviceBorrowRecord>();
        if (records == null)
            return result;

        for (int i = 0; i < records.Count; i++)
        {
            BorrowApplyApiRecord record = records[i];
            result.Add(new DeviceBorrowRecord
            {
                id = record.id,
                deviceName = record.deviceName,
                borrowerName = string.IsNullOrEmpty(record.applicantName) ? "当前用户" : record.applicantName,
                actualOutTimeStr = FormatDateLabel(record.expectedStartTime, "—"),
                actualReturnTimeStr = FormatDateLabel(record.expectedReturnTime, "—"),
                status = record.applyStatus,
                statusLabel = ResolveBorrowApplyStatus(record.applyStatus, record.applyStatusDesc),
                statusColor = ResolveBorrowApplyStatusColor(record.applyStatus),
            });
        }

        return result;
    }

    private static string ResolveBorrowApplyStatus(int status, string backendDescription)
    {
        if (!string.IsNullOrEmpty(backendDescription))
            return backendDescription;
        switch (status)
        {
            case 1: return "待审核";
            case 2: return "已通过";
            case 3: return "已驳回";
            case 4: return "已取消";
            default: return "未知";
        }
    }

    private static string ResolveBorrowApplyStatusColor(int status)
    {
        switch (status)
        {
            case 1: return "#5B9BD5";
            case 2: return "#27AE60";
            case 3: return "#E74C3C";
            case 4: return "#7A8394";
            default: return "#7A8394";
        }
    }

    private List<DeviceRepairRecord> MapRepairRecords(List<RepairApiRecord> records)
    {
        List<DeviceRepairRecord> result = new List<DeviceRepairRecord>();
        if (records == null)
            return result;

        for (int i = 0; i < records.Count; i++)
        {
            RepairApiRecord record = records[i];
            result.Add(new DeviceRepairRecord
            {
                id = record.id,
                deviceId = record.deviceId,
                deviceName = record.deviceName,
                reportTimeStr = FormatDateLabel(record.reportTime, "—"),
                description = string.IsNullOrEmpty(record.faultDesc) ? "未填写故障描述" : record.faultDesc,
                handlerName = record.handlerId > 0 ? $"处理人#{record.handlerId}" : "待分配",
                repairStatus = ResolveRepairStatus(record.repairStatus, record.repairStatusDesc),
            });
        }

        return result;
    }

    private void UpdateDerivedMonitoringData()
    {
        AttachDerivedSensorDataToDevices();
        RebuildAlertsFromCurrentData();
        RebuildLabIoTData();
        UpdateIoTStatus();
    }

    private void AttachDerivedSensorDataToDevices()
    {
        for (int i = 0; i < mockDevices.Count; i++)
        {
            DeviceData device = mockDevices[i];
            float statusPenalty = device.deviceStatus == "维修中" ? 10f : device.deviceStatus == "报废" ? 18f : device.deviceStatus == "借用中" ? 4f : 0f;
            float baseBattery = Mathf.Clamp(device.healthScore - statusPenalty, 10f, 100f);
            DeviceSensorData sensor = device.sensorData ?? new DeviceSensorData();
            sensor.deviceId = device.id;
            sensor.temperature = Mathf.Clamp(22f + statusPenalty * 0.9f, 20f, 55f);
            sensor.humidity = Mathf.Clamp(45f + statusPenalty * 0.7f, 30f, 75f);
            sensor.battery = baseBattery;
            sensor.signalStrength = Mathf.Clamp(92f - statusPenalty * 2.5f, 18f, 100f);
            sensor.lastReportStr = DateTime.Now.ToString("HH:mm:ss");
            sensor.status = ResolveDeviceSensorStatus(sensor);
            device.sensorData = sensor;
        }
    }

    private void RebuildAlertsFromCurrentData()
    {
        List<AlertRecord> alerts = new List<AlertRecord>();
        string nowTime = DateTime.Now.ToString("HH:mm");

        for (int i = 0; i < mockReagents.Count; i++)
        {
            ReagentData reagent = mockReagents[i];
            if (reagent.IsExpired)
            {
                alerts.Add(new AlertRecord
                {
                    timeStr = nowTime,
                    deviceName = reagent.name,
                    message = $"批次存在过期风险，最近有效期 {reagent.expireDateStr}",
                    level = "alarm"
                });
                continue;
            }

            if (reagent.IsLowStock)
            {
                alerts.Add(new AlertRecord
                {
                    timeStr = nowTime,
                    deviceName = reagent.name,
                    message = $"库存低于安全线，当前 {reagent.currentStock}{reagent.unit}，安全线 {reagent.safetyStock}{reagent.unit}",
                    level = "warning"
                });
            }
        }

        for (int i = 0; i < mockDevices.Count; i++)
        {
            DeviceData device = mockDevices[i];
            if (device.deviceStatus == "维修中" || device.deviceStatus == "报废")
            {
                alerts.Add(new AlertRecord
                {
                    timeStr = nowTime,
                    deviceName = device.name,
                    message = $"设备当前状态为 {device.deviceStatus}，健康分 {device.healthScore}",
                    level = device.deviceStatus == "报废" ? "alarm" : "warning"
                });
            }
            else if (device.healthScore < 60)
            {
                alerts.Add(new AlertRecord
                {
                    timeStr = nowTime,
                    deviceName = device.name,
                    message = $"设备健康分偏低（{device.healthScore}），建议尽快巡检",
                    level = "warning"
                });
            }
        }

        if (alerts.Count == 0)
        {
            alerts.Add(new AlertRecord
            {
                timeStr = nowTime,
                deviceName = "系统监控",
                message = "当前未发现异常库存或设备告警",
                level = "resolved"
            });
        }

        mockAlerts = alerts;
    }

    private void RebuildLabIoTData()
    {
        float totalHealth = 0f;
        int deviceCount = mockDevices.Count;
        int repairingCount = 0;
        int criticalCount = 0;

        for (int i = 0; i < mockDevices.Count; i++)
        {
            DeviceData device = mockDevices[i];
            totalHealth += device.healthScore;
            if (device.deviceStatus == "维修中") repairingCount++;
            if (device.deviceStatus == "报废" || device.healthScore < 45) criticalCount++;
        }

        int lowStockCount = 0;
        int expiringCount = 0;
        for (int i = 0; i < mockReagents.Count; i++)
        {
            if (mockReagents[i].IsLowStock) lowStockCount++;
            if (mockReagents[i].IsExpired || mockReagents[i].IsExpiringSoon) expiringCount++;
        }

        float avgHealth = deviceCount > 0 ? totalHealth / deviceCount : 85f;
        mockIoTData = new LabIoTData
        {
            labId = deviceCount > 0 ? mockDevices[0].labId : 0,
            temperature = Mathf.Clamp(22f + repairingCount * 1.8f + criticalCount * 0.8f, 18f, 48f),
            humidity = Mathf.Clamp(46f + lowStockCount * 1.6f, 30f, 78f),
            smoke = Mathf.Clamp(0.01f + criticalCount * 0.025f, 0.01f, 0.22f),
            gas = Mathf.Clamp(0.01f + expiringCount * 0.018f, 0.01f, 0.18f),
            battery = Mathf.Clamp(avgHealth, 10f, 100f),
            signalStrength = Mathf.Clamp(94f - repairingCount * 7f - criticalCount * 9f, 18f, 100f),
            lastUpdateStr = DateTime.Now.ToString("HH:mm:ss")
        };
    }

    private void NotifyPanelsRefreshed()
    {
        ReagentPanelUI[] reagentPanels = FindObjectsOfType<ReagentPanelUI>(true);
        for (int i = 0; i < reagentPanels.Length; i++)
            reagentPanels[i].RefreshData();

        EquipmentPanelUI[] equipmentPanels = FindObjectsOfType<EquipmentPanelUI>(true);
        for (int i = 0; i < equipmentPanels.Length; i++)
            equipmentPanels[i].RefreshData();

        MonitorPanelUI[] monitorPanels = FindObjectsOfType<MonitorPanelUI>(true);
        for (int i = 0; i < monitorPanels.Length; i++)
            monitorPanels[i].RefreshData();
    }

    private string ResolveLabName(int labId)
    {
        if (labNameMap.TryGetValue(labId, out string labName) && !string.IsNullOrEmpty(labName))
            return labName;

        return labId > 0 ? $"实验室#{labId}" : "未绑定实验室";
    }

    private static string ResolveMaterialCategory(string materialName)
    {
        if (string.IsNullOrEmpty(materialName))
            return "实验物资";
        if (materialName.Contains("酸"))
            return "无机酸";
        if (materialName.Contains("醇") || materialName.Contains("酮") || materialName.Contains("苯"))
            return "有机溶剂";
        if (materialName.Contains("氢氧化"))
            return "碱";
        if (materialName.Contains("钠") || materialName.Contains("钾") || materialName.Contains("银") || materialName.Contains("盐"))
            return "无机盐";
        return "实验物资";
    }

    private static int ResolveSafetyLevel(string materialName, string category)
    {
        if (!string.IsNullOrEmpty(materialName) && (materialName.Contains("浓") || materialName.Contains("硫酸") || materialName.Contains("盐酸")))
            return 3;
        if (category == "有机溶剂" || category == "无机酸" || category == "碱")
            return 2;
        return 1;
    }

    private static string BuildReagentSpec(MaterialApiRecord material, MaterialDetailApiRecord detail)
    {
        int batchCount = detail != null && detail.batches != null ? detail.batches.Count : material.batchCount;
        string baseCode = string.IsNullOrEmpty(material.materialNo) ? $"物资#{material.id}" : material.materialNo;
        return $"{baseCode} / 批次数 {batchCount}";
    }

    private static string BuildDeviceSpec(DeviceApiRecord record)
    {
        if (!string.IsNullOrEmpty(record.remark))
            return record.remark;
        if (!string.IsNullOrEmpty(record.deviceTypeName))
            return record.deviceTypeName;
        return "未维护设备规格";
    }

    private static string ResolveDeviceStatus(int statusCode, string statusDesc)
    {
        if (!string.IsNullOrEmpty(statusDesc))
            return statusDesc;

        switch (statusCode)
        {
            case 1: return "采购待入库";
            case 2: return "闲置";
            case 3: return "借用中";
            case 4: return "维修中";
            case 5: return "报废";
            default: return "未知";
        }
    }

    private static string ResolvePickupStatus(int statusCode, string statusDesc)
    {
        if (!string.IsNullOrEmpty(statusDesc))
            return statusDesc;

        switch (statusCode)
        {
            case 1: return "待审核";
            case 2: return "审批通过";
            case 3: return "已驳回";
            case 4: return "已取消";
            default: return "未知";
        }
    }

    private static string ResolveRepairStatus(int statusCode, string statusDesc)
    {
        if (!string.IsNullOrEmpty(statusDesc))
            return statusDesc;

        switch (statusCode)
        {
            case 1: return "待处理";
            case 2: return "维修中";
            case 3: return "已修复";
            case 4: return "已验收";
            default: return "未知";
        }
    }

    private static string NormalizeDateTime(string rawValue, string fallbackTime)
    {
        if (string.IsNullOrEmpty(rawValue))
            return string.Empty;

        string normalized = rawValue.Trim().Replace('/', '-').Replace('T', ' ');
        if (normalized.Length == 10)
            normalized = $"{normalized} {fallbackTime}";
        else if (normalized.Length == 16)
            normalized += ":00";

        if (DateTime.TryParse(normalized, out DateTime parsed))
            return parsed.ToString("yyyy-MM-dd'T'HH:mm:ss");

        return string.Empty;
    }

    private static string FormatDateLabel(string rawValue, string fallback)
    {
        if (string.IsNullOrEmpty(rawValue))
            return fallback;

        string normalized = rawValue.Replace('T', ' ');
        if (normalized.Length >= 19)
            return normalized.Substring(0, 19);
        return normalized;
    }

    // ─────────────────────────────────────────────────────────
    //  Mock 数据生成
    // ─────────────────────────────────────────────────────────

    private void GenerateMockReagents()
    {
        mockReagents = new List<ReagentData>
        {
            new ReagentData {
                id = 1, name = "氯化钠", casNo = "7647-14-5",
                spec = "AR 分析纯 / 500g", unit = "g", category = "无机盐",
                currentStock = 50f, safetyStock = 80f, location = "A-3-1 号柜",
                expireDateStr = "2027-06-30", safetyLevel = 1
            },
            new ReagentData {
                id = 2, name = "无水乙醇", casNo = "64-17-5",
                spec = "AR 分析纯 / 500mL", unit = "mL", category = "有机溶剂",
                currentStock = 200f, safetyStock = 100f, location = "B-1-2 号柜",
                expireDateStr = "2026-12-31", safetyLevel = 2
            },
            new ReagentData {
                id = 3, name = "浓硫酸", casNo = "7664-93-9",
                spec = "GR 优级纯 / 500mL", unit = "mL", category = "无机酸",
                currentStock = 30f, safetyStock = 50f, location = "C-2-3 号柜",
                expireDateStr = "2028-03-01", safetyLevel = 3
            },
            new ReagentData {
                id = 4, name = "盐酸", casNo = "7647-01-0",
                spec = "AR 分析纯 / 500mL", unit = "mL", category = "无机酸",
                currentStock = 120f, safetyStock = 60f, location = "C-2-1 号柜",
                expireDateStr = "2026-08-15", safetyLevel = 3
            },
            new ReagentData {
                id = 5, name = "碳酸钠", casNo = "497-19-8",
                spec = "AR 分析纯 / 500g", unit = "g", category = "无机盐",
                currentStock = 350f, safetyStock = 100f, location = "A-2-4 号柜",
                expireDateStr = "2028-01-01", safetyLevel = 1
            },
            new ReagentData {
                id = 6, name = "丙酮", casNo = "67-64-1",
                spec = "AR 分析纯 / 500mL", unit = "mL", category = "有机溶剂",
                currentStock = 80f, safetyStock = 100f, location = "B-1-1 号柜",
                expireDateStr = "2025-12-01", safetyLevel = 2
            },
            new ReagentData {
                id = 7, name = "氢氧化钠", casNo = "1310-73-2",
                spec = "AR 分析纯 / 500g", unit = "g", category = "碱",
                currentStock = 200f, safetyStock = 80f, location = "A-4-2 号柜",
                expireDateStr = "2028-06-01", safetyLevel = 2
            },
            new ReagentData {
                id = 8, name = "硝酸银", casNo = "7761-88-8",
                spec = "AR 分析纯 / 100g", unit = "g", category = "无机盐",
                currentStock = 15f, safetyStock = 20f, location = "D-1-1 号柜",
                expireDateStr = "2026-10-01", safetyLevel = 2
            },
        };

        // 附加批次数据
        mockReagents[0].batches = new List<ReagentBatchData>
        {
            new ReagentBatchData { id=1, reagentId=1, batchNo="B2024-001", entryDateStr="2024-03-01", entryQuantity=500f, remainQuantity=50f, status="正常" }
        };
        mockReagents[1].batches = new List<ReagentBatchData>
        {
            new ReagentBatchData { id=2, reagentId=2, batchNo="B2024-003", entryDateStr="2024-06-15", entryQuantity=500f, remainQuantity=200f, status="正常" }
        };
        mockReagents[2].batches = new List<ReagentBatchData>
        {
            new ReagentBatchData { id=3, reagentId=3, batchNo="B2025-010", entryDateStr="2025-02-01", entryQuantity=250f, remainQuantity=30f, status="低库存" }
        };
        mockReagents[3].batches = new List<ReagentBatchData>
        {
            new ReagentBatchData { id=4, reagentId=4, batchNo="B2025-021", entryDateStr="2025-04-12", entryQuantity=500f, remainQuantity=120f, status="正常" }
        };
        mockReagents[5].batches = new List<ReagentBatchData>
        {
            new ReagentBatchData { id=5, reagentId=6, batchNo="B2024-116", entryDateStr="2024-11-08", entryQuantity=500f, remainQuantity=80f, status="临期" }
        };
    }

    private void GenerateMockPickupRecords()
    {
        mockPickupRecords = new List<ReagentPickupRecord>
        {
            new ReagentPickupRecord { id = 1, reagentId = 1, reagentName = "氯化钠", quantity = 20f, unit = "g", applicantName = "王同学", pickupTimeStr = "2026-07-09 10:15", purpose = "滴定实验", status = "已领用" },
            new ReagentPickupRecord { id = 2, reagentId = 2, reagentName = "无水乙醇", quantity = 50f, unit = "mL", applicantName = "刘老师", pickupTimeStr = "2026-07-08 14:20", purpose = "样品清洗", status = "已完成" },
            new ReagentPickupRecord { id = 3, reagentId = 3, reagentName = "浓硫酸", quantity = 10f, unit = "mL", applicantName = "赵同学", pickupTimeStr = "2026-07-07 09:05", purpose = "无机分析实验", status = "审批中" }
        };
    }

    private void GenerateMockDevices()
    {
        mockDevices = new List<DeviceData>
        {
            new DeviceData {
                id = 1, deviceNo = "EQ-2025-0042", name = "生物显微镜",
                spec = "1000X / 双目 / LED光源", category = "光学仪器",
                labId = 1, labName = "生物实验室 A305",
                purchaseDateStr = "2025-03-15", purchasePrice = 8800f,
                expireDateStr = "2027-03-15", deviceStatus = "闲置", healthScore = 92
            },
            new DeviceData {
                id = 2, deviceNo = "EQ-2024-0021", name = "电子分析天平",
                spec = "量程 220g / 精度 0.0001g", category = "测量仪器",
                labId = 1, labName = "化学实验室 B201",
                purchaseDateStr = "2024-09-01", purchasePrice = 6500f,
                expireDateStr = "2026-09-01", deviceStatus = "借用中", healthScore = 85
            },
            new DeviceData {
                id = 3, deviceNo = "EQ-2023-0007", name = "台式离心机",
                spec = "最高 10000rpm / 12孔转子", category = "分离仪器",
                labId = 1, labName = "生物实验室 A305",
                purchaseDateStr = "2023-07-20", purchasePrice = 12000f,
                expireDateStr = "2025-07-20", deviceStatus = "维修中", healthScore = 45
            },
            new DeviceData {
                id = 4, deviceNo = "EQ-2025-0018", name = "恒温水浴锅",
                spec = "4孔 / 0-100°C / 数显", category = "温控设备",
                labId = 1, labName = "化学实验室 B201",
                purchaseDateStr = "2025-01-10", purchasePrice = 3200f,
                expireDateStr = "2027-01-10", deviceStatus = "闲置", healthScore = 88
            },
            new DeviceData {
                id = 5, deviceNo = "EQ-2024-0035", name = "紫外可见分光光度计",
                spec = "波长 190-1100nm / 双光束", category = "光谱仪器",
                labId = 2, labName = "分析化学实验室 C301",
                purchaseDateStr = "2024-05-20", purchasePrice = 35000f,
                expireDateStr = "2026-05-20", deviceStatus = "闲置", healthScore = 79
            },
            new DeviceData {
                id = 6, deviceNo = "EQ-2022-0004", name = "旋转蒸发仪",
                spec = "蒸发瓶 2L / 转速 0-280rpm", category = "蒸发设备",
                labId = 2, labName = "有机化学实验室 C102",
                purchaseDateStr = "2022-11-01", purchasePrice = 18000f,
                expireDateStr = "2024-11-01", deviceStatus = "报废", healthScore = 12
            },
        };
    }

    private void GenerateMockIoT()
    {
        mockIoTData = new LabIoTData
        {
            labId = 1,
            temperature = 23.5f,
            humidity = 48f,
            smoke = 0.02f,
            gas = 0.01f,
            battery = 85f,
            signalStrength = 82f,
            lastUpdateStr = DateTime.Now.ToString("HH:mm:ss"),
            status = "normal"
        };
    }

    private void GenerateMockAlerts()
    {
        mockAlerts = new List<AlertRecord>
        {
            new AlertRecord { timeStr = "14:32", deviceName = "台式离心机",
                message = "温度过高 (52°C)，已超过安全阈值 40°C", level = "alarm" },
            new AlertRecord { timeStr = "13:15", deviceName = "电子分析天平",
                message = "传感器电量过低 (42%)，建议及时充电", level = "warning" },
            new AlertRecord { timeStr = "11:00", deviceName = "恒温水浴锅",
                message = "设备状态恢复正常，温度 37°C", level = "resolved" },
            new AlertRecord { timeStr = "09:45", deviceName = "浓硫酸",
                message = "库存低于安全线，当前剩余 30mL（安全线 50mL）", level = "warning" },
            new AlertRecord { timeStr = "08:20", deviceName = "紫外可见分光光度计",
                message = "设备检测到轻微异常振动", level = "warning" },
        };
    }

    private void GenerateMockBorrowRecords()
    {
        mockBorrowRecords = new List<DeviceBorrowRecord>
        {
            new DeviceBorrowRecord { id=1, deviceName="电子分析天平", borrowerName="张同学",
                actualOutTimeStr="2026-07-08 09:00", actualReturnTimeStr="—", status=1 },
            new DeviceBorrowRecord { id=2, deviceName="生物显微镜", borrowerName="李教授",
                actualOutTimeStr="2026-07-05 14:00", actualReturnTimeStr="2026-07-06 16:00", status=2 },
            new DeviceBorrowRecord { id=3, deviceName="恒温水浴锅", borrowerName="王同学",
                actualOutTimeStr="2026-06-28 10:00", actualReturnTimeStr="—", status=3 },
            new DeviceBorrowRecord { id=4, deviceName="紫外可见分光光度计", borrowerName="赵教授",
                actualOutTimeStr="2026-07-01 08:30", actualReturnTimeStr="2026-07-03 17:00", status=2 },
        };
    }

    private void GenerateMockRepairRecords()
    {
        mockRepairRecords = new List<DeviceRepairRecord>
        {
            new DeviceRepairRecord { id = 1, deviceId = 3, deviceName = "台式离心机", reportTimeStr = "2026-07-10 14:32", description = "高速运转时温度异常升高", handlerName = "设备管理员", repairStatus = "维修中" },
            new DeviceRepairRecord { id = 2, deviceId = 2, deviceName = "电子分析天平", reportTimeStr = "2026-07-05 09:18", description = "电量衰减过快，需要检查电池模块", handlerName = "运维工程师", repairStatus = "待处理" },
            new DeviceRepairRecord { id = 3, deviceId = 4, deviceName = "恒温水浴锅", reportTimeStr = "2026-06-28 11:03", description = "温控探头校准完成", handlerName = "维修组", repairStatus = "已修复" }
        };
    }

    private void AttachSensorDataToDevices()
    {
        var sensorPool = new List<DeviceSensorData>
        {
            new DeviceSensorData { deviceId=1, temperature=22f, humidity=45f, battery=85f,
                signalStrength=88f, lastReportStr="14:30", status="normal" },
            new DeviceSensorData { deviceId=2, temperature=38f, humidity=50f, battery=42f,
                signalStrength=72f, lastReportStr="14:28", status="warning" },
            new DeviceSensorData { deviceId=3, temperature=52f, humidity=55f, battery=-1f,
                signalStrength=-1f, lastReportStr="14:32", status="alarm" },
            new DeviceSensorData { deviceId=4, temperature=37f, humidity=48f, battery=91f,
                signalStrength=90f, lastReportStr="14:35", status="normal" },
            new DeviceSensorData { deviceId=5, temperature=25f, humidity=47f, battery=78f,
                signalStrength=80f, lastReportStr="14:30", status="normal" },
            new DeviceSensorData { deviceId=6, temperature=-1f, humidity=-1f, battery=-1f,
                signalStrength=-1f, lastReportStr="—", status="normal" },
        };

        foreach (var sensor in sensorPool)
        {
            var device = mockDevices.Find(d => d.id == sensor.deviceId);
            if (device != null) device.sensorData = sensor;
        }
    }

    private void ConsumeBatchStock(ReagentData reagent, float quantity)
    {
        if (reagent.batches == null || reagent.batches.Count == 0)
            return;

        float remainToConsume = quantity;
        foreach (ReagentBatchData batch in reagent.batches)
        {
            if (remainToConsume <= 0f)
                break;

            float available = Mathf.Max(0f, batch.remainQuantity);
            float consume = Mathf.Min(available, remainToConsume);
            batch.remainQuantity -= consume;
            remainToConsume -= consume;

            if (batch.remainQuantity <= 0f)
                batch.status = "已耗尽";
            else if (batch.remainQuantity <= reagent.safetyStock)
                batch.status = "低库存";
            else
                batch.status = "正常";
        }
    }

    private void UpdateIoTStatus()
    {
        if (mockIoTData == null)
            return;

        if (mockIoTData.temperature > 40f || mockIoTData.smoke > 0.1f || mockIoTData.gas > 0.1f || mockIoTData.battery < 20f)
            mockIoTData.status = "alarm";
        else if (mockIoTData.temperature > 30f || mockIoTData.humidity < 30f || mockIoTData.humidity > 70f || mockIoTData.battery < 50f || mockIoTData.signalStrength < 60f)
            mockIoTData.status = "warning";
        else
            mockIoTData.status = "normal";
    }

    private string ResolveDeviceSensorStatus(DeviceSensorData sensor)
    {
        if (sensor == null)
            return "normal";

        if (sensor.temperature > 45f || (sensor.battery >= 0f && sensor.battery < 20f))
            return "alarm";

        if (sensor.temperature > 35f || (sensor.battery >= 0f && sensor.battery < 50f))
            return "warning";

        return "normal";
    }
}

[Serializable]
public class MaterialPageApiResponse
{
    public int code;
    public string message;
    public MaterialPageApiData data;
}

[Serializable]
public class MaterialPageApiData
{
    public List<MaterialApiRecord> records;
    public int total;
    public int pageNum;
    public int pageSize;
    public int pages;
}

[Serializable]
public class MaterialApiRecord
{
    public int id;
    public string materialNo;
    public string materialName;
    public int labId;
    public string unit;
    public int safetyStock;
    public int currentStock;
    public int batchCount;
    public string earliestExpiryDate;
    public string earliestBatchNo;
}

[Serializable]
public class MaterialDetailApiResponse
{
    public int code;
    public string message;
    public MaterialDetailApiRecord data;
}

[Serializable]
public class MaterialDetailApiRecord
{
    public int id;
    public string materialNo;
    public string materialName;
    public int labId;
    public string unit;
    public int safetyStock;
    public int currentStock;
    public List<MaterialBatchApiRecord> batches;
}

[Serializable]
public class MaterialBatchApiRecord
{
    public int id;
    public string batchNo;
    public int materialId;
    public string materialName;
    public int quantity;
    public int remaining;
    public string expiryDate;
    public string stockInTime;
    public int batchStatus;
    public string batchStatusDesc;
    public string remark;
}

[Serializable]
public class ReceivePageApiResponse
{
    public int code;
    public string message;
    public ReceivePageApiData data;
}

[Serializable]
public class ReceivePageApiData
{
    public List<ReceiveApplyApiRecord> records;
    public int total;
    public int pageNum;
    public int pageSize;
    public int pages;
}

[Serializable]
public class ReceiveApplyApiRecord
{
    public int id;
    public string applyNo;
    public int materialId;
    public string materialName;
    public int applicantId;
    public int approverId;
    public int expectedQuantity;
    public string purpose;
    public int applyStatus;
    public string applyStatusDesc;
    public string rejectReason;
    public string createdAt;
    public string updatedAt;
}

[Serializable]
public class DevicePageApiResponse
{
    public int code;
    public string message;
    public DevicePageApiData data;
}

[Serializable]
public class DevicePageApiData
{
    public List<DeviceApiRecord> records;
    public int total;
    public int pageNum;
    public int pageSize;
    public int pages;
}

[Serializable]
public class DeviceApiRecord
{
    public int id;
    public string deviceNo;
    public string name;
    public int deviceTypeId;
    public string deviceTypeName;
    public int labId;
    public string purchaseDate;
    public float purchasePrice;
    public string expireDate;
    public int deviceStatus;
    public string deviceStatusDesc;
    public int healthScore;
    public int version;
    public string remark;
}

[Serializable]
public class BorrowApplyPageApiResponse
{
    public int code;
    public string message;
    public BorrowApplyPageApiData data;
}

[Serializable]
public class BorrowApplyPageApiData
{
    public List<BorrowApplyApiRecord> records;
    public int total;
    public int pageNum;
    public int pageSize;
    public int pages;
}

[Serializable]
public class BorrowApplyApiRecord
{
    public int id;
    public int deviceId;
    public string deviceNo;
    public string deviceName;
    public int applicantId;
    public string applicantName;
    public int approverId;
    public string expectedStartTime;
    public string expectedReturnTime;
    public string purpose;
    public int applyStatus;
    public string applyStatusDesc;
    public string rejectReason;
    public int isOverdue;
    public string createdAt;
    public string updatedAt;
}

[Serializable]
public class RepairPageApiResponse
{
    public int code;
    public string message;
    public RepairPageApiData data;
}

[Serializable]
public class RepairPageApiData
{
    public List<RepairApiRecord> records;
    public int total;
    public int pageNum;
    public int pageSize;
    public int pages;
}

[Serializable]
public class RepairApiRecord
{
    public int id;
    public string repairNo;
    public int deviceId;
    public string deviceName;
    public int reporterId;
    public int handlerId;
    public string reportTime;
    public string faultDesc;
    public int repairStatus;
    public string repairStatusDesc;
    public string repairDesc;
    public string completedAt;
    public string acceptedAt;
    public string createdAt;
    public string updatedAt;
}

[Serializable]
public class MaterialReceiveApplyApiRequest
{
    public int materialId;
    public int expectedQuantity;
    public string purpose;
}

[Serializable]
public class DeviceBorrowApplyApiRequest
{
    public string expectedStartTime;
    public string expectedReturnTime;
    public string purpose;
}

[Serializable]
public class DeviceRepairReportApiRequest
{
    public int deviceId;
    public string faultDesc;
}

// ── 实验室 IoT 汇总 API 响应（对接后端 GET /api/iot/labs/{labId}/summary）──

[Serializable]
public class LabIotSummaryApiResponse
{
    public int code;
    public string message;
    public LabIotSummaryResponse data;
}

[Serializable]
public class LabIotSummaryResponse
{
    public long labId;
    public float temperature;
    public float humidity;
    public float smoke;
    public float gas;
    public float battery;
    public float signalStrength;
    public string status;
    public string lastUpdateStr;
    public SensorSummaryItem[] sensors;
}

[Serializable]
public class SensorSummaryItem
{
    public long sensorId;
    public string sensorNo;
    public string sensorTypeName;
    public string dataName;
    public float value;
    public string unit;
}

[Serializable]
public class SensorDataPostRequest
{
    public string sensorNo;
    public string detectedAt;
    public SensorDetectedData detectedData;
}

[Serializable]
public class SensorDetectedData
{
    public float temperature;
    public float humidity;
}
