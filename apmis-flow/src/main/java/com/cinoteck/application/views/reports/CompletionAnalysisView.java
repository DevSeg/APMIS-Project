package com.cinoteck.application.views.reports;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import com.flowingcode.vaadin.addons.gridexporter.GridExporter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.MultiSortPriority;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.api.campaign.data.CampaignFormDataIndexDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.FormAccess;
import de.symeda.sormas.api.utils.SortProperty;

@Route(layout = ReportView.class)
public class CompletionAnalysisView extends VerticalLayout implements RouterLayout{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6242228590115105671L;
	private ComboBox<CampaignReferenceDto> campaign = new ComboBox<>();
	private ComboBox<AreaReferenceDto> regionFilter = new ComboBox<>();
	private ComboBox<RegionReferenceDto> provinceFilter = new ComboBox<>();
	private ComboBox<DistrictReferenceDto> districtFilter = new ComboBox<>();
	private Button resetButton;

	List<CampaignReferenceDto> campaigns;
	List<CampaignReferenceDto> campaignPhases;
	List<AreaReferenceDto> regions;
	List<RegionReferenceDto> provinces;
	List<DistrictReferenceDto> districts;
	CampaignFormDataCriteria criteria;
	Grid<CampaignFormDataIndexDto> grid = new Grid<>(CampaignFormDataIndexDto.class, false);
	
	DataProvider<CampaignFormDataIndexDto, CampaignFormDataCriteria> dataProvider;
	FormAccess formAccess;
	Button exportReport = new Button();
	Anchor anchor = new Anchor("", I18nProperties.getCaption(Captions.export));
	Icon icon = VaadinIcon.UPLOAD_ALT.create();
	
	
	
    private void refreshGridData(FormAccess formAccess) {
    	int numberOfRows = FacadeProvider.getCampaignFormDataFacade().prepareAllCompletionAnalysis();
    	dataProvider = DataProvider
				.fromFilteringCallbacks(
						query -> FacadeProvider.getCampaignFormDataFacade()
								.getByCompletionAnalysis(criteria, query.getOffset(),
										query.getLimit(),
										query.getSortOrders().stream()
												.map(sortOrder -> new SortProperty(sortOrder.getSorted(),
														sortOrder.getDirection() == SortDirection.ASCENDING))
												.collect(Collectors.toList()), formAccess)
								.stream(), 
						query -> Integer.parseInt(FacadeProvider.getCampaignFormDataFacade()
						.getByCompletionAnalysisCount(criteria, query.getOffset(),
						query.getLimit(),
						query.getSortOrders().stream()
								.map(sortOrder -> new SortProperty(sortOrder.getSorted(),
										sortOrder.getDirection() == SortDirection.ASCENDING))
								.collect(Collectors.toList()), formAccess)));

        grid.setDataProvider(dataProvider);
    }
	
	public CompletionAnalysisView() {
		
		this.criteria = new CampaignFormDataCriteria();
		
		setHeightFull();
		
		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setPadding(false);
		filterLayout.setVisible(true);
		filterLayout.setAlignItems(Alignment.END);

		campaign.setLabel(I18nProperties.getCaption(Captions.Campaigns));
		campaign.setPlaceholder(I18nProperties.getCaption(Captions.campaignAllCampaigns));
		campaigns = FacadeProvider.getCampaignFacade().getAllActiveCampaignsAsReference();
		campaign.setItems(campaigns);
		CampaignReferenceDto lastStarted = FacadeProvider.getCampaignFacade().getLastStartedCampaign();
		campaign.setValue(lastStarted);
		 criteria.campaign(lastStarted);
		campaign.addValueChangeListener(e-> {
			 CampaignReferenceDto selectedCAmpaign = e.getValue();
			   if (selectedCAmpaign != null) {
				   criteria.campaign(selectedCAmpaign);
				   refreshGridData(formAccess);
			   }else {
				   criteria.campaign(null);
				   refreshGridData(formAccess);
			   }
			   
		});
		

		regionFilter.setLabel(I18nProperties.getCaption(Captions.area));
		regionFilter.setPlaceholder(I18nProperties.getCaption(Captions.areaAllAreas));
		regionFilter.setItems(FacadeProvider.getAreaFacade().getAllActiveAsReference());
		regionFilter.setClearButtonVisible(true);
		regionFilter.addValueChangeListener(e -> {
            AreaReferenceDto selectedArea = e.getValue();
            if (selectedArea != null) {
                provinces = FacadeProvider.getRegionFacade().getAllActiveByArea(selectedArea.getUuid());
                provinceFilter.setItems(provinces);
                criteria.campaign(campaign.getValue());
                criteria.area(selectedArea);
                
                refreshGridData(formAccess);
            }
            else {
              	 criteria.area(null);
   	            refreshGridData(formAccess);
              }
        });


		
		provinceFilter.setLabel(I18nProperties.getCaption(Captions.region));
		provinceFilter.setPlaceholder(I18nProperties.getCaption(Captions.regionAllRegions));
//		provinceFilter.setItems(FacadeProvider.getRegionFacade().getAllActiveAsReference());
		provinceFilter.addValueChangeListener(e -> {
            RegionReferenceDto selectedRegion = e.getValue();
            if (selectedRegion != null) {
                districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(selectedRegion.getUuid());
                districtFilter.setItems(districts);
                criteria.region(selectedRegion);
                refreshGridData(formAccess);
            }else {
           	 criteria.region(null);
	            refreshGridData(formAccess);
           }
        });

		
		districtFilter.setLabel(I18nProperties.getCaption(Captions.district));
		districtFilter.setPlaceholder(I18nProperties.getCaption(Captions.districtAllDistricts));
//		districtFilter.setItems(FacadeProvider.getDistrictFacade().getAllActiveAsReference());
		   districtFilter.addValueChangeListener(e -> {
	            DistrictReferenceDto selectedDistrict = e.getValue();
	            if (selectedDistrict != null) {
	                criteria.district(selectedDistrict);
	                refreshGridData(formAccess);
	            }else {
	            	 criteria.district(null);
	 	            refreshGridData(formAccess);

	            }
	        });
		resetButton =  new Button(I18nProperties.getCaption(Captions.actionResetFilters));
		resetButton.addClickListener(e->{
			campaign.clear();
			provinceFilter.clear();
			districtFilter.clear();
			regionFilter.clear();
			criteria.area(null);
			criteria.region(null);
			criteria.district(null);


			  refreshGridData(formAccess);
	
		});
		
		
		Button displayFilters = new Button(I18nProperties.getCaption(Captions.showFilters), new Icon(VaadinIcon.SLIDERS));
		displayFilters.addClickListener(e->{
			if(filterLayout.isVisible() == false) {
				filterLayout.setVisible(true);
				displayFilters.setText(I18nProperties.getCaption(Captions.hideFilters));
			}else {
				filterLayout.setVisible(false);
				displayFilters.setText(I18nProperties.getCaption(Captions.showFilters));
			}
		});
		
		exportReport.setText(I18nProperties.getCaption(Captions.export));
		exportReport.addClickListener(e -> {
			anchor.getElement().callJsFunction("click");
		});
		anchor.getStyle().set("display", "none");

		
		
		filterLayout.setClassName("row pl-3");
		filterLayout.add(campaign, regionFilter, provinceFilter, districtFilter, resetButton, exportReport, anchor);
		
		
		
		HorizontalLayout layout = new HorizontalLayout(); 
		layout.setAlignItems(Alignment.END);
		layout.getStyle().set("margin-left", "15px");
		layout.add(displayFilters, filterLayout);
		
		add(layout);
		
		completionAnalysisGrid(criteria, formAccess);
		
	}
	
	public void reload() {
		grid.getDataProvider().refreshAll();
		criteria.campaign(campaign.getValue());
		criteria.area(regionFilter.getValue());
//		criteria.setFormType(campaignPhase.getValue().toString());
//		criteria.setCampaignFormMeta(campaignFormCombo.getValue());
//		criteria.area(regionCombo.getValue());
//		criteria.region(provinceCombo.getValue());
//		criteria.district(districtCombo.getValue());
//		criteria.community(clusterCombo.getValue());
	}
	
	@SuppressWarnings("deprecation")
	private void completionAnalysisGrid(CampaignFormDataCriteria criteria, FormAccess formAccess) {
		
		grid.setSelectionMode(SelectionMode.SINGLE);
		grid.setMultiSort(true, MultiSortPriority.APPEND);
		grid.setSizeFull();
		grid.setColumnReorderingAllowed(true);

//		grid.addColumn(CampaignFormDataIndexDto::getCampaign).setHeader(I18nProperties.getCaption(Captions.Campaigns)).setSortable(true).setResizable(true);

		grid.addColumn(CampaignFormDataIndexDto::getArea).setHeader(I18nProperties.getCaption(Captions.area)).setSortable(true).setResizable(true);
		grid.addColumn(CampaignFormDataIndexDto::getRegion).setHeader(I18nProperties.getCaption(Captions.region)).setSortable(true).setResizable(true);
		grid.addColumn(CampaignFormDataIndexDto::getDistrict).setHeader(I18nProperties.getCaption(Captions.district)).setSortable(true).setResizable(true);
		grid.addColumn(CampaignFormDataIndexDto::getCcode).setHeader(I18nProperties.getCaption(Captions.Community_externalID)).setSortable(true).setResizable(true);
		grid.addColumn(CampaignFormDataIndexDto::getClusternumber).setHeader(I18nProperties.getCaption(Captions.clusterNumber)).setSortable(true).setResizable(true);

		grid.addColumn(CampaignFormDataIndexDto::getAnalysis_a).setHeader(I18nProperties.getCaption(Captions.icmSupervisorMonitoring)).setSortable(true).setResizable(true);
		grid.addColumn(CampaignFormDataIndexDto::getAnalysis_b).setHeader(I18nProperties.getCaption(Captions.icmRevisits)).setSortable(true).setResizable(true);
		grid.addColumn(CampaignFormDataIndexDto::getAnalysis_c).setHeader(I18nProperties.getCaption(Captions.icmHouseholdMonitoring)).setSortable(true).setResizable(true);
		grid.addColumn(CampaignFormDataIndexDto::getAnalysis_d).setHeader(I18nProperties.getCaption(Captions.icmTeamMonitoring)).setSortable(true).setResizable(true);


		grid.setVisible(true);
//		int numberOfRows = FacadeProvider.getCampaignFormDataFacade()
//				.getByCompletionAnalysisCount(null, null, null, null,formAccess );
		int numberOfRows = FacadeProvider.getCampaignFormDataFacade().prepareAllCompletionAnalysis();
		dataProvider = DataProvider
				.fromFilteringCallbacks(
						query -> FacadeProvider.getCampaignFormDataFacade()
								.getByCompletionAnalysis(criteria, query.getOffset(),
										query.getLimit(),
										query.getSortOrders().stream()
												.map(sortOrder -> new SortProperty(sortOrder.getSorted(),
														sortOrder.getDirection() == SortDirection.ASCENDING))
												.collect(Collectors.toList()), null)
								.stream(), 
						query -> numberOfRows
								);
		grid.setDataProvider(dataProvider);
		
		GridExporter<CampaignFormDataIndexDto> exporter = GridExporter.createFor(grid);
		exporter.setAutoAttachExportButtons(false);

		exporter.setTitle(I18nProperties.getCaption(Captions.campaignDataInformation));
		exporter.setFileName(
				"Completion Analysis Report" + new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime()));

		anchor.setHref(exporter.getCsvStreamResource());
		anchor.getElement().setAttribute("download", true);
		anchor.setClassName("exportJsonGLoss");
		anchor.setId("campDatAnchor");

		anchor.getStyle().set("width", "100px");

		icon.getStyle().set("margin-right", "8px");
		icon.getStyle().set("font-size", "10px");

		anchor.getElement().insertChild(0, icon.getElement());
		add(grid);
		
	}

}