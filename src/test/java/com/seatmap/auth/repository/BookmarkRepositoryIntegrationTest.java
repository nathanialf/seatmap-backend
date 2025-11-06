package com.seatmap.auth.repository;

import com.seatmap.common.model.Bookmark;
import com.seatmap.common.repository.DynamoDbRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for BookmarkRepository focusing on the functionality
 * we fixed: null handling, serialization of computed properties, etc.
 */
@DisplayName("BookmarkRepository Serialization Tests")
public class BookmarkRepositoryIntegrationTest {

    private TestBookmarkRepository repository;

    // Test implementation of BookmarkRepository for testing serialization
    private static class TestBookmarkRepository extends DynamoDbRepository<Bookmark> {
        public TestBookmarkRepository() {
            super(null, "test-bookmarks");
        }

        @Override
        protected Class<Bookmark> getEntityClass() {
            return Bookmark.class;
        }

        @Override
        protected String getHashKeyName() {
            return "userId";
        }

        @Override
        protected String getRangeKeyName() {
            return "bookmarkId";
        }

        // Expose protected methods for testing
        public Map<String, AttributeValue> testToAttributeValueMap(Bookmark entity) throws Exception {
            return super.toAttributeValueMap(entity);
        }

        public Bookmark testFromAttributeValueMap(Map<String, AttributeValue> attributeMap) throws Exception {
            return super.fromAttributeValueMap(attributeMap);
        }
    }

    @BeforeEach
    void setUp() {
        repository = new TestBookmarkRepository();
    }

    @Test
    @DisplayName("Should serialize bookmark without computed properties")
    void saveBookmark_ShouldNotSerializeComputedProperties() throws Exception {
        // Create a bookmark with all fields set
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-bookmark");
        bookmark.setTitle("Test Bookmark");
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);
        bookmark.setFlightOfferData("{\"test\": \"data\"}");
        bookmark.setCreatedAt(Instant.now());
        bookmark.setUpdatedAt(Instant.now());
        bookmark.setExpiresAt(Instant.now().plusSeconds(3600));

        // Convert to attribute value map (this tests our @JsonIgnore fix)
        Map<String, AttributeValue> attributeMap = repository.testToAttributeValueMap(bookmark);

        // Verify computed properties are NOT included
        assertFalse(attributeMap.containsKey("isBookmark"), 
            "@JsonIgnore should prevent isBookmark from being serialized");
        assertFalse(attributeMap.containsKey("isSavedSearch"), 
            "@JsonIgnore should prevent isSavedSearch from being serialized");
        assertFalse(attributeMap.containsKey("isExpired"), 
            "@JsonIgnore should prevent isExpired from being serialized");

        // Verify essential fields ARE included
        assertTrue(attributeMap.containsKey("userId"));
        assertTrue(attributeMap.containsKey("bookmarkId"));
        assertTrue(attributeMap.containsKey("title"));
        assertTrue(attributeMap.containsKey("itemType"));
        assertTrue(attributeMap.containsKey("flightOfferData"));
        assertTrue(attributeMap.containsKey("createdAt"));
        assertTrue(attributeMap.containsKey("expiresAt"));
    }

    @Test
    @DisplayName("Should handle null searchRequest field for regular bookmarks")
    void saveBookmark_WithNullSearchRequest_ShouldNotThrowException() throws Exception {
        // Regular bookmarks have null searchRequest (only saved searches use this field)
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-bookmark");
        bookmark.setTitle("Test Bookmark");
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);
        bookmark.setFlightOfferData("{\"test\": \"data\"}");
        bookmark.setSearchRequest(null); // This should not cause NPE
        bookmark.setCreatedAt(Instant.now());
        bookmark.setExpiresAt(Instant.now().plusSeconds(3600));

        // This should not throw NPE (tests our null safety fix)
        assertDoesNotThrow(() -> {
            Map<String, AttributeValue> attributeMap = repository.testToAttributeValueMap(bookmark);
            assertNotNull(attributeMap);
            
            // Verify null searchRequest is handled gracefully
            if (attributeMap.containsKey("searchRequest")) {
                assertTrue(attributeMap.get("searchRequest").nul(),
                    "Null searchRequest should be properly serialized as DynamoDB NULL");
            }
        });
    }

    @Test
    @DisplayName("Should handle saved search with null flightOfferData")
    void saveSavedSearch_WithNullFlightOfferData_ShouldNotThrowException() throws Exception {
        // Saved searches have null flightOfferData (only regular bookmarks use this field)
        Bookmark savedSearch = new Bookmark();
        savedSearch.setUserId("test-user");
        savedSearch.setBookmarkId("test-search");
        savedSearch.setTitle("Test Saved Search");
        savedSearch.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        savedSearch.setFlightOfferData(null); // This should not cause NPE
        savedSearch.setCreatedAt(Instant.now());
        savedSearch.setExpiresAt(Instant.now().plusSeconds(3600));

        // This should not throw NPE (tests our null safety fix)
        assertDoesNotThrow(() -> {
            Map<String, AttributeValue> attributeMap = repository.testToAttributeValueMap(savedSearch);
            assertNotNull(attributeMap);
            
            // Verify null flightOfferData is handled gracefully
            if (attributeMap.containsKey("flightOfferData")) {
                assertTrue(attributeMap.get("flightOfferData").nul(),
                    "Null flightOfferData should be properly serialized as DynamoDB NULL");
            }
        });
    }

    @Test
    @DisplayName("Should correctly identify bookmark vs saved search types")
    void bookmarkTypes_ShouldBeCorrectlyIdentified() {
        Bookmark bookmark = new Bookmark();
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);

        Bookmark savedSearch = new Bookmark();
        savedSearch.setItemType(Bookmark.ItemType.SAVED_SEARCH);

        // Test computed properties work correctly
        assertTrue(bookmark.isBookmark());
        assertFalse(bookmark.isSavedSearch());
        
        assertFalse(savedSearch.isBookmark());
        assertTrue(savedSearch.isSavedSearch());
    }

    @Test
    @DisplayName("Should correctly compute expiration status")
    void expirationStatus_ShouldBeCorrectlyComputed() {
        Bookmark expiredBookmark = new Bookmark();
        expiredBookmark.setExpiresAt(Instant.now().minusSeconds(3600)); // 1 hour ago

        Bookmark activeBookmark = new Bookmark();
        activeBookmark.setExpiresAt(Instant.now().plusSeconds(3600)); // 1 hour from now

        Bookmark noExpiryBookmark = new Bookmark();
        noExpiryBookmark.setExpiresAt(null);

        // Test computed expiration status
        assertTrue(expiredBookmark.isExpired());
        assertFalse(activeBookmark.isExpired());
        assertFalse(noExpiryBookmark.isExpired()); // null expiresAt means not expired
    }

    @Test
    @DisplayName("Should handle mixed null and non-null fields")
    void serialization_WithMixedNullFields_ShouldWork() throws Exception {
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-bookmark");
        bookmark.setTitle("Test Bookmark");
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);
        bookmark.setFlightOfferData("{\"test\": \"data\"}");
        bookmark.setSearchRequest(null); // Null field
        bookmark.setCreatedAt(Instant.now());
        bookmark.setUpdatedAt(null); // Another null field
        bookmark.setExpiresAt(Instant.now().plusSeconds(3600));
        bookmark.setLastAccessedAt(null); // Yet another null field

        // Should handle the mix of null and non-null fields gracefully
        assertDoesNotThrow(() -> {
            Map<String, AttributeValue> attributeMap = repository.testToAttributeValueMap(bookmark);
            assertNotNull(attributeMap);
            
            // Verify non-null fields are properly serialized
            assertEquals("test-user", attributeMap.get("userId").s());
            assertEquals("BOOKMARK", attributeMap.get("itemType").s());
            
            // Verify null fields are handled properly (either not included or as DynamoDB NULL)
            if (attributeMap.containsKey("searchRequest")) {
                assertTrue(attributeMap.get("searchRequest").nul());
            }
            if (attributeMap.containsKey("updatedAt")) {
                assertTrue(attributeMap.get("updatedAt").nul());
            }
            if (attributeMap.containsKey("lastAccessedAt")) {
                assertTrue(attributeMap.get("lastAccessedAt").nul());
            }
        });
    }

    @Test
    @DisplayName("Round-trip serialization should preserve all actual fields")
    void roundTripSerialization_ShouldPreserveAllActualFields() throws Exception {
        // Use truncated timestamps to avoid nanosecond precision issues
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-bookmark");
        bookmark.setTitle("Test Bookmark");
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);
        bookmark.setFlightOfferData("{\"test\": \"data\"}");
        bookmark.setCreatedAt(now);
        bookmark.setUpdatedAt(now.plusSeconds(1));
        bookmark.setExpiresAt(now.plusSeconds(3600));

        // When
        Map<String, AttributeValue> attributeMap = repository.testToAttributeValueMap(bookmark);
        Bookmark deserialized = repository.testFromAttributeValueMap(attributeMap);

        // Then
        assertEquals(bookmark.getUserId(), deserialized.getUserId());
        assertEquals(bookmark.getBookmarkId(), deserialized.getBookmarkId());
        assertEquals(bookmark.getTitle(), deserialized.getTitle());
        assertEquals(bookmark.getItemType(), deserialized.getItemType());
        assertEquals(bookmark.getFlightOfferData(), deserialized.getFlightOfferData());
        assertEquals(bookmark.getCreatedAt(), deserialized.getCreatedAt());
        assertEquals(bookmark.getUpdatedAt(), deserialized.getUpdatedAt());
        assertEquals(bookmark.getExpiresAt(), deserialized.getExpiresAt());
    }

    @Test
    @DisplayName("Computed properties should work after deserialization")
    void computedProperties_ShouldWorkAfterDeserialization() throws Exception {
        Bookmark bookmark = new Bookmark();
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);
        bookmark.setExpiresAt(Instant.now().plusSeconds(3600)); // Not expired

        Bookmark savedSearch = new Bookmark();
        savedSearch.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        savedSearch.setExpiresAt(Instant.now().minusSeconds(3600)); // Expired

        // When - round trip
        Map<String, AttributeValue> bookmarkMap = repository.testToAttributeValueMap(bookmark);
        Bookmark deserializedBookmark = repository.testFromAttributeValueMap(bookmarkMap);

        Map<String, AttributeValue> searchMap = repository.testToAttributeValueMap(savedSearch);
        Bookmark deserializedSearch = repository.testFromAttributeValueMap(searchMap);

        // Then - computed properties should work correctly
        assertTrue(deserializedBookmark.isBookmark());
        assertFalse(deserializedBookmark.isSavedSearch());
        assertFalse(deserializedBookmark.isExpired());

        assertFalse(deserializedSearch.isBookmark());
        assertTrue(deserializedSearch.isSavedSearch());
        assertTrue(deserializedSearch.isExpired());
    }
}