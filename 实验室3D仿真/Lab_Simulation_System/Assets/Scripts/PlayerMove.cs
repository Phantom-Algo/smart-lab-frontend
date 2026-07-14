using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class PlayerMove : MonoBehaviour
{
    [Header("References")]
    public Transform cameraTransform;
    public Transform playerBody;
    public CharacterController characterController;

    [Header("Move")]
    public float moveSpeed = 5f;
    public float gravity = -9.81f;
    public float inputDeadZone = 0.1f;

    [Header("Look")]
    public float mouseSensitivity = 2f;
    public float maxLookAngle = 80f;

    [Header("Camera Follow Fallback")]
    public bool forceCameraFollowBody = false;
    public Vector3 cameraLocalOffset = new Vector3(0f, 1.6f, 0f);
    public bool autoCenterCameraXZ = true;

    private float xRotation = 0f;
    private float yVelocity = 0f;
    private bool hasLoggedConfigError = false;
    private bool hasLoggedSetupInfo = false;

    /// <summary>面板打开时设为 false，屏蔽玩家移动和视角输入</summary>
    public bool inputEnabled = true;

    void Start() {
        if (characterController == null) {
            characterController = GetComponent<CharacterController>();
            if (characterController == null) {
                characterController = GetComponentInParent<CharacterController>();
            }
        }

        if (cameraTransform == null) {
            Camera camInChildren = GetComponentInChildren<Camera>();
            if (camInChildren != null) {
                cameraTransform = camInChildren.transform;
            } else if (Camera.main != null) {
                cameraTransform = Camera.main.transform;
            }
        }

        AutoFixReferences();
        AutoFixCameraLocalOffset();
        LogCurrentSetup();

        Cursor.lockState = CursorLockMode.Locked;
        Cursor.visible = false;
    }

    void Update() {
        Look();
        Move();
    }

    void LateUpdate() {
        if (!forceCameraFollowBody) return;
        if (!HasValidSetup()) return;
        if (cameraTransform.parent == playerBody) return;

        // Fallback only when camera is not parented under player body.
        cameraTransform.position = playerBody.position + playerBody.TransformVector(cameraLocalOffset);
    }

    void Look() {
        if (!HasValidSetup()) return;
        if (!inputEnabled) return;

        float mouseX = Input.GetAxis("Mouse X") * mouseSensitivity;
        float mouseY = Input.GetAxis("Mouse Y") * mouseSensitivity;

        playerBody.Rotate(Vector3.up * mouseX, Space.Self);

        xRotation -= mouseY;
        xRotation = Mathf.Clamp(xRotation, -maxLookAngle, maxLookAngle);

        cameraTransform.localRotation = Quaternion.Euler(xRotation, 0f, 0f);
    }

    void Move() {
        if (!HasValidSetup()) return;
        if (!inputEnabled) return;

        float h = Input.GetAxisRaw("Horizontal");
        float v = Input.GetAxisRaw("Vertical");

        // Use body yaw direction only, so camera pitch won't affect movement.
        Vector3 forward = playerBody.forward;
        Vector3 right = playerBody.right;
        forward.y = 0f;
        right.y = 0f;
        forward.Normalize();
        right.Normalize();

        Vector2 input = new Vector2(h, v);
        if (input.magnitude < inputDeadZone) {
            input = Vector2.zero;
        } else if (input.magnitude > 1f) {
            input.Normalize();
        }

        Vector3 move = right * input.x + forward * input.y;
        characterController.Move(move * moveSpeed * Time.deltaTime);

        if(characterController.isGrounded && yVelocity < 0f) {
            yVelocity = -2f;
        }

        yVelocity += gravity * Time.deltaTime;
        characterController.Move(Vector3.up * yVelocity * Time.deltaTime);
    }

    bool HasValidSetup() {
        AutoFixReferences();
        AutoFixCameraLocalOffset();

        if (characterController != null && playerBody != null && cameraTransform != null) {
            hasLoggedConfigError = false;
            return true;
        }

        if (!hasLoggedConfigError) {
            Debug.LogError("PlayerMove 引用缺失：请绑定 CharacterController / PlayerBody / CameraTransform。");
            hasLoggedConfigError = true;
        }
        return false;
    }

    void AutoFixReferences() {
        if (characterController == null) return;

        if (playerBody == null) {
            playerBody = characterController.transform;
        }

        // If body is camera itself, use a safer parent/root as body.
        if (cameraTransform != null && playerBody == cameraTransform) {
            if (characterController.transform != cameraTransform) {
                playerBody = characterController.transform;
            } else if (cameraTransform.parent != null) {
                playerBody = cameraTransform.parent;
            }
        }
    }

    void AutoFixCameraLocalOffset() {
        if (!autoCenterCameraXZ) return;
        if (cameraTransform == null || playerBody == null) return;
        if (cameraTransform.parent != playerBody) return;

        Vector3 localPos = cameraTransform.localPosition;
        if (Mathf.Abs(localPos.x) > 0.001f || Mathf.Abs(localPos.z) > 0.001f) {
            localPos.x = 0f;
            localPos.z = 0f;
            cameraTransform.localPosition = localPos;
            Debug.LogWarning("PlayerMove: 已自动将 Camera 的本地 X/Z 归零，避免旋转时出现绕圈位移。");
        }
    }

    void LogCurrentSetup() {
        if (hasLoggedSetupInfo) return;
        hasLoggedSetupInfo = true;

        string camName = cameraTransform != null ? cameraTransform.name : "NULL";
        string bodyName = playerBody != null ? playerBody.name : "NULL";
        string ccName = characterController != null ? characterController.name : "NULL";
        Debug.Log($"PlayerMove Setup -> Camera: {camName}, Body: {bodyName}, Controller: {ccName}");
    }
}
