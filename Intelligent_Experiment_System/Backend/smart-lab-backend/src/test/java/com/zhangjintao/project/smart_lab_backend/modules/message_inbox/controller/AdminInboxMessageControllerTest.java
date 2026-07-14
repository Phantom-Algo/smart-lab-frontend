package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.BatchDeleteInboxMessageRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.InboxMessagePageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageListItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminInboxMessageControllerTest {

	@Test
	void controllerRequiresAdminRole() {
		PreAuthorize preAuthorize = AdminInboxMessageController.class.getAnnotation(PreAuthorize.class);

		assertThat(preAuthorize).isNotNull();
		assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
	}

	@Test
	void pageDelegatesToServiceWithTargetUserId() {
		InboxMessageControllerTest.RecordingInboxMessageManagementService service =
				new InboxMessageControllerTest.RecordingInboxMessageManagementService();
		AdminInboxMessageController controller = new AdminInboxMessageController(service);
		InboxMessagePageQueryRequest request = new InboxMessagePageQueryRequest();
		request.setPageNum(3L);
		request.setPageSize(30L);

		ApiResult<PageResult<InboxMessageListItemResponse>> result = controller.page(99L, request);

		assertThat(service.pageUserId).isEqualTo(99L);
		assertThat(service.pageUserInboxRequest).isSameAs(request);
		assertThat(result.getData().getPageNum()).isEqualTo(3L);
	}

	@Test
	void getDetailDelegatesToServiceWithoutMyInboxPath() {
		InboxMessageControllerTest.RecordingInboxMessageManagementService service =
				new InboxMessageControllerTest.RecordingInboxMessageManagementService();
		AdminInboxMessageController controller = new AdminInboxMessageController(service);

		ApiResult<InboxMessageDetailResponse> result = controller.getDetail(99L, 10L);

		assertThat(service.getUserId).isEqualTo(99L);
		assertThat(service.getUserInboxMessageId).isEqualTo(10L);
		assertThat(result.getData().getInboxMessageId()).isEqualTo(10L);
	}

	@Test
	void deleteDelegatesToServiceWithTargetUserId() {
		InboxMessageControllerTest.RecordingInboxMessageManagementService service =
				new InboxMessageControllerTest.RecordingInboxMessageManagementService();
		AdminInboxMessageController controller = new AdminInboxMessageController(service);

		controller.delete(99L, 11L);

		assertThat(service.deleteUserId).isEqualTo(99L);
		assertThat(service.deleteUserInboxMessageId).isEqualTo(11L);
	}

	@Test
	void deleteBatchDelegatesToServiceWithTargetUserId() {
		InboxMessageControllerTest.RecordingInboxMessageManagementService service =
				new InboxMessageControllerTest.RecordingInboxMessageManagementService();
		AdminInboxMessageController controller = new AdminInboxMessageController(service);
		BatchDeleteInboxMessageRequest request = new BatchDeleteInboxMessageRequest();
		request.setInboxMessageIds(List.of(1L, 2L));

		controller.deleteBatch(99L, request);

		assertThat(service.deleteBatchUserId).isEqualTo(99L);
		assertThat(service.deleteUserMessagesRequest).isSameAs(request);
	}
}
