package br.com.messenger.gae_service.repository;

import br.com.messenger.gae_service.exception.ProductOfInterestNotFoundException;
import br.com.messenger.gae_service.exception.UserNotFoundException;
import br.com.messenger.gae_service.model.ProductOfInterest;
import com.google.appengine.api.datastore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.logging.Logger;

@Repository
public class ProductOfInterestRepository {

    private static final Logger log = Logger.getLogger(ProductOfInterestRepository.class.getName());

    private DatastoreService datastoreService;

    @Autowired
    public ProductOfInterestRepository() {
        this.datastoreService = DatastoreServiceFactory.getDatastoreService();
    }

    private static final String USER_KIND = "users";

    private static final String PRODUCT_OF_INTEREST_KIND = "productsOfInterest";
    private static final String PRODUCT_OF_INTEREST_KEY = "productOfInterestKey";

    private static final String PROPERTY_PRODUCT_OF_INTEREST_ID = "productOfInterestId";
    private static final String PROPERTY_CPF = "cpf";
    private static final String PROPERTY_SALES_PROVIDER_USER_ID = "salesProviderUserId";
    private static final String PROPERTY_SALES_PROVIDER_PRODUCT_ID = "salesProviderProductId";
    private static final String PROPERTY_MIN_PRICE_ALERT = "minPriceAlert";

    public List<ProductOfInterest> getProductsOfInterestBySalesProviderProductIdAndMinPriceAlert(Long salesProviderProductId, double minPriceAlert) {

        List<ProductOfInterest> productsOfInterest = new ArrayList<>();

        Query.Filter salesProviderProductIdFilter = new Query.FilterPredicate(PROPERTY_SALES_PROVIDER_PRODUCT_ID, Query.FilterOperator.EQUAL, salesProviderProductId);
        Query.Filter minPriceFilter = new Query.FilterPredicate(PROPERTY_MIN_PRICE_ALERT, Query.FilterOperator.GREATER_THAN_OR_EQUAL, minPriceAlert);
        Query.CompositeFilter minPriceAndSalesProviderProductIdFilter = Query.CompositeFilterOperator.and(salesProviderProductIdFilter, minPriceFilter);
        Query query = new Query(PRODUCT_OF_INTEREST_KIND).setFilter(minPriceAndSalesProviderProductIdFilter);
        List<Entity> productOfInterestEntities = datastoreService.prepare(query).asList(FetchOptions.Builder.withDefaults());

        for (Entity productOfInterestEntity : productOfInterestEntities)
            productsOfInterest.add(entityToProductOfInterest(productOfInterestEntity));

        return productsOfInterest;
    }

    public List<ProductOfInterest> getProductsOfInterestByCpf(String cpf) throws UserNotFoundException {

        if (!checkIfUserExistsByCpf(cpf))
            throw new UserNotFoundException("Usuário com cpf: " + cpf + " não encontrado");

        List<ProductOfInterest> productsOfInterest = new ArrayList<>();

        Query.Filter filter = new Query.FilterPredicate(PROPERTY_CPF, Query.FilterOperator.EQUAL, cpf);
        Query query = new Query(PRODUCT_OF_INTEREST_KIND).setFilter(filter);
        List<Entity> productOfInterestEntities = datastoreService.prepare(query).asList(FetchOptions.Builder.withDefaults());

        for (Entity productOfInterestEntity : productOfInterestEntities)
            productsOfInterest.add(entityToProductOfInterest(productOfInterestEntity));

        return productsOfInterest;
    }

    public ProductOfInterest saveProductOfInterest(ProductOfInterest productOfInterest) throws UserNotFoundException {

        if (!checkIfUserExistsByCpf(productOfInterest.getCpf()))
            throw new UserNotFoundException("Usuário com cpf: " + productOfInterest.getCpf() + " não encontrado");

        Entity productOfInterestEntity = getProductOfInterestEntityByCpfAndSalesProviderProductId(productOfInterest.getCpf(), productOfInterest.getSalesProviderProductId());

        if (productOfInterestEntity == null) {
            Key productOfInterestKey = KeyFactory.createKey(PRODUCT_OF_INTEREST_KIND, PRODUCT_OF_INTEREST_KEY);
            productOfInterestEntity = new Entity(PRODUCT_OF_INTEREST_KIND, productOfInterestKey);
        }

        productOfInterestToEntity(productOfInterest, productOfInterestEntity);
        datastoreService.put(productOfInterestEntity);
        return entityToProductOfInterest(productOfInterestEntity);
    }

    public ProductOfInterest deleteProductOfInterest(String cpf, Long salesProviderProductId) throws ProductOfInterestNotFoundException {

        Entity productOfInterestEntity = getProductOfInterestEntityByCpfAndSalesProviderProductId(cpf, salesProviderProductId);

        if (productOfInterestEntity != null) {
            datastoreService.delete(productOfInterestEntity.getKey());
            return entityToProductOfInterest(productOfInterestEntity);
        } else {
            throw new ProductOfInterestNotFoundException("Produto de Interesse com cpf: " + cpf + " e salesProviderProductId: " + salesProviderProductId + "  não encontrado");
        }
    }

    private boolean checkIfUserExistsByCpf(String cpf) {
        Query.Filter filter = new Query.FilterPredicate(PROPERTY_CPF, Query.FilterOperator.EQUAL, cpf);
        Query query = new Query(USER_KIND).setFilter(filter);
        Entity userEntity = datastoreService.prepare(query).asSingleEntity();
        return (userEntity != null);
    }

    private Entity getProductOfInterestEntityByCpfAndSalesProviderProductId(String cpf, Long salesProviderProductId) {
        Query.Filter cpfFilter = new Query.FilterPredicate(PROPERTY_CPF, Query.FilterOperator.EQUAL, cpf);
        Query.Filter salesProviderProductIdFilter = new Query.FilterPredicate(PROPERTY_SALES_PROVIDER_PRODUCT_ID, Query.FilterOperator.EQUAL, salesProviderProductId);
        Query.CompositeFilter cpfAndSalesProviderProductIdFilter = Query.CompositeFilterOperator.and(cpfFilter, salesProviderProductIdFilter);
        Query query = new Query(PRODUCT_OF_INTEREST_KIND).setFilter(cpfAndSalesProviderProductIdFilter);
        return datastoreService.prepare(query).asSingleEntity();
    }

    private void productOfInterestToEntity(ProductOfInterest productOfInterest, Entity productOfInterestEntity) {
        productOfInterestEntity.setProperty(PROPERTY_PRODUCT_OF_INTEREST_ID, productOfInterest.getProductOfInterestId());
        productOfInterestEntity.setProperty(PROPERTY_CPF, productOfInterest.getCpf());
        productOfInterestEntity.setProperty(PROPERTY_SALES_PROVIDER_USER_ID, productOfInterest.getSalesProviderUserId());
        productOfInterestEntity.setProperty(PROPERTY_SALES_PROVIDER_PRODUCT_ID, productOfInterest.getSalesProviderProductId());
        productOfInterestEntity.setProperty(PROPERTY_MIN_PRICE_ALERT, productOfInterest.getMinPriceAlert());
    }

    private ProductOfInterest entityToProductOfInterest(Entity productOfInterestEntity) {
        ProductOfInterest productOfInterest = new ProductOfInterest();
        productOfInterest.setProductOfInterestId(productOfInterestEntity.getKey().getId());
        productOfInterest.setCpf((String) productOfInterestEntity.getProperty(PROPERTY_CPF));
        productOfInterest.setSalesProviderUserId((Long) productOfInterestEntity.getProperty(PROPERTY_SALES_PROVIDER_USER_ID));
        productOfInterest.setSalesProviderProductId((Long) productOfInterestEntity.getProperty(PROPERTY_SALES_PROVIDER_PRODUCT_ID));
        productOfInterest.setMinPriceAlert((Double) productOfInterestEntity.getProperty(PROPERTY_MIN_PRICE_ALERT));

        return productOfInterest;
    }
}
