package com.cinoteck.application.views.useractivitysummary;

import java.util.List;

import com.cinoteck.application.views.reports.ReportView;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.MultiSortPriority;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignIndexDto;
import de.symeda.sormas.api.campaign.statistics.CampaignStatisticsDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserActivitySummaryDto;

@SuppressWarnings("serial")
@Route(layout = UserActivitySummary.class)
public class LoginReportView extends VerticalLayout implements RouterLayout {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6692702413655392041L;
	private Grid<UserActivitySummaryDto> grid = new Grid<>(UserActivitySummaryDto.class, false);

	public LoginReportView() {
		setSizeFull();
		setHeightFull();
		confiureLoginActivityGrid();
	}

	public void confiureLoginActivityGrid() {
		grid.setSelectionMode(SelectionMode.NONE);
		grid.setMultiSort(true, MultiSortPriority.APPEND);
		grid.setSizeFull();
		grid.setHeightFull();
		grid.setColumnReorderingAllowed(true);

		grid.addColumn(UserActivitySummaryDto.ACTION_MODULE).setHeader(I18nProperties.getCaption(Captions.Campaign_endDate))
				.setSortable(true).setResizable(true);
		grid.addColumn(UserActivitySummaryDto.AUDIT_USER).setHeader(I18nProperties.getCaption(Captions.Campaign_endDate))
		.setSortable(true).setResizable(true);
		grid.addColumn(UserActivitySummaryDto.ACTION_logged).setHeader(I18nProperties.getCaption(Captions.campaignYear))
				.setSortable(true).setResizable(true);
//		grid.addColumn(UserActivitySummaryDto.CREATIONdATE).setHeader(I18nProperties.getCaption(Captions.Campaign_endDate))
//		.setSortable(true).setResizable(true);
		
		List<UserActivitySummaryDto> dataProvider = FacadeProvider.getUserFacade().getUsersActivityByModule("login");

		grid.setItems(dataProvider);
		
		add(grid);
	}

}
