package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgTemplateDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper.MsgTemplateMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TemplateStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageTemplateRepositoryImplTest {

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void pageReturnsConvertedDomainPage() {
		MsgTemplateMapper mapper = mock(MsgTemplateMapper.class);
		MsgTemplateRepositoryImpl repository = new MsgTemplateRepositoryImpl(mapper);
		Page<MsgTemplateDO> mapperPage = new Page<>(2, 20);
		mapperPage.setRecords(List.of(templateDO()));
		mapperPage.setTotal(31);
		when(mapper.selectPage(any(Page.class), any())).thenReturn(mapperPage);

		PageResult<MessageTemplate> result = repository.page(2, 20,
				"message_inbox", "实验室", "message_inbox.normal.lab_notice", TemplateStatus.ENABLED);

		assertThat(result.getTotal()).isEqualTo(31);
		assertThat(result.getPageNum()).isEqualTo(2);
		assertThat(result.getPageSize()).isEqualTo(20);
		assertThat(result.getRecords()).hasSize(1);
		assertThat(result.getRecords().get(0).getTemplateCode()).isEqualTo("message_inbox.normal.lab_notice");
		assertThat(result.getRecords().get(0).getContentFormat()).isEqualTo(ContentFormat.TEXT);
		ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
		verify(mapper).selectPage(pageCaptor.capture(), any());
		assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
		assertThat(pageCaptor.getValue().getSize()).isEqualTo(20);
	}

	private MsgTemplateDO templateDO() {
		MsgTemplateDO templateDO = new MsgTemplateDO();
		templateDO.setId(1L);
		templateDO.setTemplateCode("message_inbox.normal.lab_notice");
		templateDO.setTemplateName("实验室通知");
		templateDO.setMessageType("message_inbox.normal.lab_notice");
		templateDO.setTitleTemplate("实验室通知");
		templateDO.setContentTemplate("实验室开放时间调整");
		templateDO.setContentFormat(ContentFormat.TEXT.getCode());
		templateDO.setStatus(TemplateStatus.ENABLED.getCode());
		return templateDO;
	}
}
