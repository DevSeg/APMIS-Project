package de.symeda.sormas.backend.campaign;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.campaign.CampaignCriteria;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.backend.common.AbstractCoreAdoService;
import de.symeda.sormas.backend.common.AbstractDomainObject;
import de.symeda.sormas.backend.common.CriteriaBuilderHelper;
import de.symeda.sormas.backend.user.User;


@Stateless
@LocalBean
public class CampaignService extends AbstractCoreAdoService<Campaign> {

	public CampaignService() {
		super(Campaign.class);
	}

	/**
	 * a user who has access to @CamnpaignView can read all campaigns
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Predicate createUserFilter(CriteriaBuilder cb, CriteriaQuery cq, From<?, Campaign> from) {
		return null;
	}

	public Predicate buildCriteriaFilter(CampaignCriteria campaignCriteria, CriteriaBuilder cb, Root<Campaign> from) {

		Predicate filter = null;
		if (campaignCriteria.getDeleted() != null) {
			filter = CriteriaBuilderHelper.and(cb, filter, cb.equal(from.get(Campaign.DELETED), campaignCriteria.getDeleted()));
		}
		if (campaignCriteria.getStartDateAfter() != null || campaignCriteria.getStartDateBefore() != null) {
			filter = CriteriaBuilderHelper.and(
				cb,
				filter,
				cb.between(from.get(Campaign.START_DATE), campaignCriteria.getStartDateAfter(), campaignCriteria.getStartDateBefore()));
		}
		if (campaignCriteria.getEndDateAfter() != null || campaignCriteria.getEndDateBefore() != null) {
			filter = CriteriaBuilderHelper.and(cb, filter, cb.between(from.get(Campaign.END_DATE), campaignCriteria.getEndDateAfter(), campaignCriteria.getEndDateAfter()));
		}
		if (campaignCriteria.getFreeText() != null) {
			String[] textFilters = campaignCriteria.getFreeText().split("\\s+");
			for (String textFilter : textFilters) {
				if (DataHelper.isNullOrEmpty(textFilter)) {
					continue;
				}

				Predicate likeFilters = cb.or(
						CriteriaBuilderHelper.unaccentedIlike(cb, from.get(Campaign.NAME), textFilter),
						CriteriaBuilderHelper.ilike(cb, from.get(Campaign.UUID), textFilter),
				CriteriaBuilderHelper.unaccentedIlike(cb, from.get(Campaign.CAMPAIGN_YEAR), textFilter));
				filter = CriteriaBuilderHelper.and(cb, filter, likeFilters);
			}
		}
		if (campaignCriteria.getRelevanceStatus() != null) {
			if (campaignCriteria.getRelevanceStatus() == EntityRelevanceStatus.ACTIVE) {
				filter = CriteriaBuilderHelper.and(cb, filter, cb.or(cb.equal(from.get(Campaign.ARCHIVED), false), cb.isNull(from.get(Campaign.ARCHIVED))));
			} else if (campaignCriteria.getRelevanceStatus() == EntityRelevanceStatus.ARCHIVED) {
				filter = CriteriaBuilderHelper.and(cb, filter, cb.equal(from.get(Campaign.ARCHIVED), true));
			}
		}
		return filter;
	}

	public List<String> getAllActiveUuids() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<String> cq = cb.createQuery(String.class);
		Root<Campaign> from = cq.from(getElementClass());

		Predicate filter = cb.and();

		if (getCurrentUser() != null) {
			Predicate userFilter = createUserFilter(cb, cq, from);
			filter = CriteriaBuilderHelper.and(cb, createActiveCampaignsFilter(cb, from), userFilter);
		}

		cq.where(filter);
		cq.select(from.get(Campaign.UUID));

		return em.createQuery(cq).getResultList();
	}

	public List<Campaign> getAllAfter(Date since, User user) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Campaign> cq = cb.createQuery(getElementClass());
		Root<Campaign> root = cq.from(getElementClass());

		Predicate filter = createUserFilter(cb, cq, root);
		if (since != null) {
			Predicate dateFilter = createChangeDateFilter(cb, root, since);
			if (filter != null) {
				filter = cb.and(filter, dateFilter);
			} else {
				filter = dateFilter;
			}
		}
		if (filter != null) {
			filter = cb.and(filter, createActiveCampaignsFilter(cb, root));
			cq.where(filter);
		} else {
		cq.where(createActiveCampaignsFilter(cb, root));
		}

		cq.orderBy(cb.desc(root.get(AbstractDomainObject.CHANGE_DATE)));

		return em.createQuery(cq).getResultList();
	}

	public List<Campaign> getAllActive() {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Campaign> cq = cb.createQuery(getElementClass());
		Root<Campaign> from = cq.from(getElementClass());
		cq.where(createActiveCampaignsFilter(cb, from));
		cq.orderBy(cb.desc(from.get(AbstractDomainObject.CHANGE_DATE)));

		return em.createQuery(cq).getResultList();
	}
	
	public int cloneForm(Campaign uuidx, String userCreatingx) {
		
		Random r = new Random();
		int low = 10;
		int high = 600;
		int result = r.nextInt(high-low) + low;
		
		Random rx = new Random();
		int lowx = 10;
		int highx = 100;
		int resultx = rx.nextInt(highx-lowx) + lowx;
		
		
		
		String cdv = "";
		
		//String cdc = "insert into campaigns (SELECT CAST(CONCAT('-1',id,'"+result+"') AS bigint) as id, CONCAT(uuid,'-DUP') as uuid, changedate, creationdate, CONCAT(name,'-DUP'), description, startdate, enddate, creatinguser_id, deleted, archived, sys_period, dashboardelements, cluster, round FROM campaigns where name='"+uuidx+"')";
		try{ 
			String cds = cloneFormx1x(uuidx, result, resultx, userCreatingx);
		}finally{
			cdv = "insert into campaign_campaignformmeta (SELECT CAST(CONCAT('"+resultx+"',dc.id,'"+result+"') AS bigint) as id, cd.campaignformmeta_id, cd.sys_period FROM campaigns dc inner join campaign_campaignformmeta cd on (dc.id = cd.campaign_id) where dc.name='"+uuidx+"' and deleted = false)";
			
			System.out.println(cdv+"++++++++++++++ccccccccccc++++++6++++++++++++++++++++");
			
		}
		return em.createNativeQuery(cdv).executeUpdate();
	}
	
	
	public String cloneFormx1x(Campaign uuidx, int unixd, int uuiss, String userCreating) {
		
		
		
		UUID uuisd = UUID.randomUUID();
		
		String cdc = "insert into campaigns (SELECT CAST(CONCAT('"+uuiss+"',id,'"+unixd+"') AS bigint) as id, '"+uuisd.toString().toUpperCase()+"' as uuid, changedate, creationdate, CONCAT(name,'-DUP'), description, startdate, enddate, (select id from users where uuid = '"+userCreating+"' limit 1), deleted, archived, sys_period, dashboardelements, cluster, round, campaignyear FROM campaigns where name='"+uuidx+"' and archived = false and  deleted = false)";
		System.out.println(cdc);
		em.createNativeQuery(cdc).executeUpdate();
		
		return uuisd.toString().toUpperCase(); 
	}
	
	/*public int cloneFormx(Campaign uuidx, int unix) {
		String cdv = "insert into campaign_campaignformmeta (SELECT CAST(CONCAT('-1',dc.id,'"+unix+"') AS bigint) as id, cd.campaignformmeta_id, cd.sys_period FROM campaigns dc inner join campaign_campaignformmeta cd on (dc.id = cd.campaign_id) where dc.name='"+uuidx+"')";
		return em.createNativeQuery(cdv).executeUpdate();
	}*/

	public Predicate createActiveCampaignsFilter(CriteriaBuilder cb, Root<Campaign> root) {
		return cb.and(cb.isFalse(root.get(Campaign.ARCHIVED)), cb.isFalse(root.get(Campaign.DELETED)));
	}
}
