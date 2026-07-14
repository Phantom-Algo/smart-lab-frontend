using UnityEditor;
using UnityEngine;

/// <summary>
/// 一键给 GroupStudyRoom 场景中的所有墙壁（PlainWall / WallDoor / WallWindow）添加 BoxCollider。
/// 使用方法：菜单栏 → Tools → 添加墙壁碰撞体
/// </summary>
public class AddWallColliders : EditorWindow
{
    [MenuItem("Tools/添加墙壁碰撞体", false, 200)]
    public static void AddCollidersToWalls()
    {
        // 查找场景中所有名称包含墙壁关键词的 GameObject
        string[] wallKeywords = { "PlainWall", "WallDoor", "WallWindow", "WallDoorFlipped" };

        int addedCount = 0;
        int skippedCount = 0;

        GameObject[] allObjects = GameObject.FindObjectsOfType<GameObject>(true);

        foreach (GameObject go in allObjects)
        {
            if (go == null) continue;

            string name = go.name;
            bool isWall = false;
            foreach (string keyword in wallKeywords)
            {
                if (name.Contains(keyword))
                {
                    isWall = true;
                    break;
                }
            }

            if (!isWall) continue;

            // 检查是否已有 Collider
            Collider existingCollider = go.GetComponent<Collider>();
            if (existingCollider != null)
            {
                skippedCount++;
                continue;
            }

            // 添加 BoxCollider
            BoxCollider boxCollider = go.AddComponent<BoxCollider>();

            // 尝试根据 MeshFilter 的 mesh 自动适配大小
            MeshFilter meshFilter = go.GetComponent<MeshFilter>();
            if (meshFilter != null && meshFilter.sharedMesh != null)
            {
                Bounds bounds = meshFilter.sharedMesh.bounds;
                boxCollider.center = bounds.center;
                boxCollider.size = bounds.size;
            }

            // 标记为 dirty 以便保存
            EditorUtility.SetDirty(go);

            addedCount++;
            Debug.Log($"[AddWallColliders] 已为 '{name}' 添加 BoxCollider");
        }

        // 保存场景
        UnityEditor.SceneManagement.EditorSceneManager.MarkSceneDirty(
            UnityEditor.SceneManagement.EditorSceneManager.GetActiveScene());

        EditorUtility.DisplayDialog(
            "完成",
            $"已为 {addedCount} 个墙壁添加 BoxCollider。\n跳过 {skippedCount} 个（已有 Collider）。\n\n请保存场景（Ctrl+S / Cmd+S）。",
            "确定");
    }
}
