package com.bookstore.service;

import com.bookstore.dao.AppUserDao;
import com.bookstore.dao.BookstoreDao;
import com.bookstore.dao.OrderDao;
import com.bookstore.dao.StoreInventoryDao;
import com.bookstore.dto.AssignOwnerRequest;
import com.bookstore.dto.CreateStoreRequest;
import com.bookstore.dto.StoreSummaryDto;
import com.bookstore.entity.AppRole;
import com.bookstore.entity.AppUser;
import com.bookstore.entity.Bookstore;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing multi-store bookstore provisioning, owner scoping, and branch performance metrics.
 */
public class StoreService {

    private static final Logger logger = LoggerFactory.getLogger(StoreService.class);

    private final BookstoreDao bookstoreDao;
    private final StoreInventoryDao storeInventoryDao;
    private final AppUserDao appUserDao;
    private final OrderDao orderDao;

    public StoreService() {
        this(new BookstoreDao(), new StoreInventoryDao(), new AppUserDao(), new OrderDao());
    }

    public StoreService(BookstoreDao bookstoreDao, StoreInventoryDao storeInventoryDao, AppUserDao appUserDao, OrderDao orderDao) {
        this.bookstoreDao = bookstoreDao;
        this.storeInventoryDao = storeInventoryDao;
        this.appUserDao = appUserDao;
        this.orderDao = orderDao;
    }

    /**
     * Creates a new bookstore branch with address, contact details, and geo-coordinates.
     */
    public Bookstore createStore(CreateStoreRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Store creation payload cannot be null.");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Store name is required.");
        }

        Bookstore store = new Bookstore(
                req.getName().trim(),
                req.getStreet() != null ? req.getStreet().trim() : null,
                req.getCity() != null ? req.getCity().trim() : null,
                req.getPincode() != null ? req.getPincode().trim() : null,
                req.getPhone() != null ? req.getPhone().trim() : null,
                req.getEmail() != null ? req.getEmail().trim() : null,
                req.getLatitude(),
                req.getLongitude()
        );
        if (req.getActive() != null) {
            store.setActive(req.getActive());
        }

        Bookstore saved = bookstoreDao.save(store);
        logger.info("Admin created new bookstore branch: ID={}, Name='{}', City='{}'",
                saved.getStoreId(), saved.getName(), saved.getCity());
        return saved;
    }

    /**
     * Retrieves all stores for admin oversight, enriched with owner details and operational metrics.
     */
    public List<StoreSummaryDto> listStoresForAdmin() {
        List<Bookstore> stores = bookstoreDao.findAll();
        List<StoreSummaryDto> summaries = new ArrayList<>();

        for (Bookstore store : stores) {
            StoreSummaryDto dto = new StoreSummaryDto();
            dto.setStoreId(store.getStoreId());
            dto.setName(store.getName());
            dto.setStreet(store.getStreet());
            dto.setCity(store.getCity());
            dto.setPincode(store.getPincode());
            dto.setPhone(store.getPhone());
            dto.setEmail(store.getEmail());
            dto.setLatitude(store.getLatitude());
            dto.setLongitude(store.getLongitude());
            dto.setActive(store.getActive());
            dto.setCreatedAt(store.getCreatedAt());

            // Manager / Owner lookup
            Optional<AppUser> ownerOpt = appUserDao.findByAssignedStoreId(store.getStoreId());
            if (ownerOpt.isPresent()) {
                dto.setOwnerId(ownerOpt.get().getId());
                dto.setOwnerEmail(ownerOpt.get().getEmail());
            } else {
                dto.setOwnerId(null);
                dto.setOwnerEmail(null);
            }

            // Inventory metrics
            dto.setTotalTitles(storeInventoryDao.countDistinctTitlesByStore(store.getStoreId()));
            dto.setTotalStock(storeInventoryDao.sumStockByStore(store.getStoreId()));

            // Order metrics
            dto.setTotalOrders(orderDao.countOrdersByStoreId(store.getStoreId()));
            BigDecimal revenue = orderDao.sumRevenueByStoreId(store.getStoreId());
            dto.setTotalRevenue(revenue != null ? revenue : BigDecimal.ZERO);

            summaries.add(dto);
        }

        return summaries;
    }

    /**
     * Assigns an existing user or provisions a new user with BOOKSTORE_OWNER role bound to a specific bookstore branch.
     */
    public AppUser assignOwnerToStore(Long storeId, AssignOwnerRequest req) {
        if (storeId == null) {
            throw new IllegalArgumentException("Store ID must be specified.");
        }
        Bookstore store = bookstoreDao.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Bookstore not found with ID: " + storeId));

        if (req == null || ((req.getEmail() == null || req.getEmail().isBlank()) && req.getUserId() == null)) {
            throw new IllegalArgumentException("User email or user ID must be provided to assign owner.");
        }

        AppUser user;
        if (req.getUserId() != null) {
            user = appUserDao.findById(req.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + req.getUserId()));
            user.setRole(AppRole.BOOKSTORE_OWNER);
            user.setAssignedStore(store);
            user = appUserDao.update(user);
            logger.info("Assigned existing user ID={} to Store ID={}", user.getId(), store.getStoreId());
        } else {
            String cleanEmail = req.getEmail().trim().toLowerCase();
            Optional<AppUser> existingOpt = appUserDao.findByEmail(cleanEmail);

            if (existingOpt.isPresent()) {
                user = existingOpt.get();
                user.setRole(AppRole.BOOKSTORE_OWNER);
                user.setAssignedStore(store);
                user = appUserDao.update(user);
                logger.info("Updated existing user '{}' to BOOKSTORE_OWNER for Store ID={}", cleanEmail, store.getStoreId());
            } else {
                String password = req.getPassword() != null && !req.getPassword().isBlank() ? req.getPassword() : "Owner@123";
                if (password.length() < 6) {
                    throw new IllegalArgumentException("Password must be at least 6 characters long.");
                }
                String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
                user = new AppUser(cleanEmail, hash, AppRole.BOOKSTORE_OWNER);
                user.setAssignedStore(store);
                user = appUserDao.save(user);
                logger.info("Provisioned new BOOKSTORE_OWNER user '{}' for Store ID={}", cleanEmail, store.getStoreId());
            }
        }

        return user;
    }

    public Optional<Bookstore> findById(Long storeId) {
        return bookstoreDao.findById(storeId);
    }

    public List<Bookstore> findActiveStores() {
        return bookstoreDao.findActiveStores();
    }
}
