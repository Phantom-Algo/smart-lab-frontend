package com.zhangjintao.project.smart_lab_backend.modules.lab_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateLabTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateLabTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabTypeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LabTypeControllerTest {

	@Test
	void adminWriteMethodsRequireAdminRole() throws NoSuchMethodException {
		Method create = LabTypeController.class.getMethod("create", CreateLabTypeRequest.class);
		Method update = LabTypeController.class.getMethod("update", Long.class, UpdateLabTypeRequest.class);
		Method delete = LabTypeController.class.getMethod("delete", Long.class);

		assertThat(create.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(update.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(delete.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
	}

	@Test
	void listAllDoesNotRequireAdminRole() throws NoSuchMethodException {
		Method listAll = LabTypeController.class.getMethod("listAll");

		assertThat(listAll.getAnnotation(PreAuthorize.class)).isNull();
	}

	@Test
	void createDelegatesToService() {
		RecordingLabTypeService service = new RecordingLabTypeService();
		LabTypeController controller = new LabTypeController(service);
		CreateLabTypeRequest request = new CreateLabTypeRequest();
		request.setLabTypeName(" 化学实验室 ");

		ApiResult<LabTypeResponse> result = controller.create(request);

		assertThat(service.createCommand.getLabTypeName()).isEqualTo("化学实验室");
		assertThat(result.getData().getId()).isEqualTo(1L);
	}

	@Test
	void updateDelegatesToService() {
		RecordingLabTypeService service = new RecordingLabTypeService();
		LabTypeController controller = new LabTypeController(service);
		UpdateLabTypeRequest request = new UpdateLabTypeRequest();
		request.setLabTypeName("物理实验室");

		ApiResult<LabTypeResponse> result = controller.update(2L, request);

		assertThat(service.updateTypeId).isEqualTo(2L);
		assertThat(service.updateCommand.getLabTypeName()).isEqualTo("物理实验室");
		assertThat(result.getData().getId()).isEqualTo(2L);
	}

	@Test
	void deleteDelegatesToService() {
		RecordingLabTypeService service = new RecordingLabTypeService();
		LabTypeController controller = new LabTypeController(service);

		controller.delete(3L);

		assertThat(service.deleteTypeId).isEqualTo(3L);
	}

	@Test
	void listAllDelegatesToService() {
		RecordingLabTypeService service = new RecordingLabTypeService();
		LabTypeController controller = new LabTypeController(service);

		ApiResult<List<LabTypeResponse>> result = controller.listAll();

		assertThat(service.listAllCalled).isTrue();
		assertThat(result.getData()).extracting(LabTypeResponse::getLabTypeName).containsExactly("化学实验室");
	}

	private static class RecordingLabTypeService implements LabTypeService {

		private LabTypeCreateCommand createCommand;
		private Long updateTypeId;
		private LabTypeUpdateCommand updateCommand;
		private Long deleteTypeId;
		private boolean listAllCalled;

		@Override
		public LabTypeResponse create(LabTypeCreateCommand command) {
			this.createCommand = command;
			return response(1L, command.getLabTypeName());
		}

		@Override
		public LabTypeResponse update(Long typeId, LabTypeUpdateCommand command) {
			this.updateTypeId = typeId;
			this.updateCommand = command;
			return response(typeId, command.getLabTypeName());
		}

		@Override
		public void delete(Long typeId) {
			this.deleteTypeId = typeId;
		}

		@Override
		public List<LabTypeResponse> listAll() {
			this.listAllCalled = true;
			return List.of(response(1L, "化学实验室"));
		}

		private LabTypeResponse response(Long id, String labTypeName) {
			return LabTypeResponse.builder()
					.id(id)
					.labTypeName(labTypeName)
					.build();
		}
	}
}
