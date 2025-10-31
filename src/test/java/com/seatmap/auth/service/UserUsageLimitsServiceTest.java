package com.seatmap.auth.service;

import com.seatmap.auth.repository.UserUsageRepository;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.User.AccountTier;
import com.seatmap.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUsageLimitsServiceTest {
    
    @Mock
    private UserUsageRepository mockUsageRepository;
    
    @Mock
    private DynamoDbClient mockDynamoDbClient;
    
    private UserUsageLimitsService service;
    private final String testUserId = "test-user-123";
    
    @BeforeEach
    void setUp() {
        // Service will be initialized with tier definitions where needed
        service = null; // Create service per test as needed
    }
    
    private void setupMockTierDefinitions() {
        // Mock tier definitions scan response
        Map<String, AttributeValue> freeItem = new HashMap<>();
        freeItem.put("tierId", AttributeValue.builder().s("free-us-2025").build());
        freeItem.put("tierName", AttributeValue.builder().s("FREE").build());
        freeItem.put("maxBookmarks", AttributeValue.builder().n("0").build());
        freeItem.put("maxSeatmapCalls", AttributeValue.builder().n("10").build());
        freeItem.put("active", AttributeValue.builder().bool(true).build());
        
        Map<String, AttributeValue> proItem = new HashMap<>();
        proItem.put("tierId", AttributeValue.builder().s("pro-us-2025").build());
        proItem.put("tierName", AttributeValue.builder().s("PRO").build());
        proItem.put("maxBookmarks", AttributeValue.builder().n("50").build());
        proItem.put("maxSeatmapCalls", AttributeValue.builder().n("500").build());
        proItem.put("active", AttributeValue.builder().bool(true).build());
        
        Map<String, AttributeValue> businessItem = new HashMap<>();
        businessItem.put("tierId", AttributeValue.builder().s("business-us-2025").build());
        businessItem.put("tierName", AttributeValue.builder().s("BUSINESS").build());
        businessItem.put("maxBookmarks", AttributeValue.builder().n("-1").build());
        businessItem.put("maxSeatmapCalls", AttributeValue.builder().n("-1").build());
        businessItem.put("canDowngrade", AttributeValue.builder().bool(false).build());
        businessItem.put("active", AttributeValue.builder().bool(true).build());
        
        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(freeItem, proItem, businessItem)
            .build();
            
        when(mockDynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);
    }
    
    private User createTestUser(AccountTier tier) {
        User user = new User();
        user.setUserId(testUserId);
        user.setEmail("test@example.com");
        user.setAccountTier(tier);
        return user;
    }
    
    @Test
    void canCreateBookmark_FreeUser_WithinLimit_ShouldReturnFalse() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User freeUser = createTestUser(AccountTier.FREE);
        when(mockUsageRepository.canCreateBookmark(testUserId, 0)).thenReturn(false);
        
        // Act
        boolean result = service.canCreateBookmark(freeUser);
        
        // Assert
        assertFalse(result);
        verify(mockUsageRepository).canCreateBookmark(testUserId, 0);
    }
    
    @Test
    void canCreateBookmark_ProUser_WithinLimit_ShouldReturnTrue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockUsageRepository.canCreateBookmark(testUserId, 50)).thenReturn(true);
        
        // Act
        boolean result = service.canCreateBookmark(proUser);
        
        // Assert
        assertTrue(result);
        verify(mockUsageRepository).canCreateBookmark(testUserId, 50);
    }
    
    @Test
    void canCreateBookmark_ProUser_ExceedsLimit_ShouldReturnFalse() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockUsageRepository.canCreateBookmark(testUserId, 50)).thenReturn(false);
        
        // Act
        boolean result = service.canCreateBookmark(proUser);
        
        // Assert
        assertFalse(result);
        verify(mockUsageRepository).canCreateBookmark(testUserId, 50);
    }
    
    @Test
    void canCreateBookmark_BusinessUser_ShouldReturnTrue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User businessUser = createTestUser(AccountTier.BUSINESS);
        when(mockUsageRepository.canCreateBookmark(testUserId, -1)).thenReturn(true);
        
        // Act
        boolean result = service.canCreateBookmark(businessUser);
        
        // Assert
        assertTrue(result);
        verify(mockUsageRepository).canCreateBookmark(testUserId, -1);
    }
    
    @Test
    void canCreateBookmark_ExceptionThrown_ShouldReturnFalse() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockUsageRepository.canCreateBookmark(anyString(), anyInt()))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act
        boolean result = service.canCreateBookmark(proUser);
        
        // Assert
        assertFalse(result); // Fail closed for safety
    }
    
    @Test
    void canMakeSeatmapRequest_FreeUser_WithinLimit_ShouldReturnTrue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User freeUser = createTestUser(AccountTier.FREE);
        when(mockUsageRepository.canMakeSeatmapRequest(testUserId, 10)).thenReturn(true);
        
        // Act
        boolean result = service.canMakeSeatmapRequest(freeUser);
        
        // Assert
        assertTrue(result);
        verify(mockUsageRepository).canMakeSeatmapRequest(testUserId, 10);
    }
    
    @Test
    void canMakeSeatmapRequest_ProUser_WithinLimit_ShouldReturnTrue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockUsageRepository.canMakeSeatmapRequest(testUserId, 500)).thenReturn(true);
        
        // Act
        boolean result = service.canMakeSeatmapRequest(proUser);
        
        // Assert
        assertTrue(result);
        verify(mockUsageRepository).canMakeSeatmapRequest(testUserId, 500);
    }
    
    @Test
    void canMakeSeatmapRequest_BusinessUser_ShouldReturnTrue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User businessUser = createTestUser(AccountTier.BUSINESS);
        when(mockUsageRepository.canMakeSeatmapRequest(testUserId, -1)).thenReturn(true);
        
        // Act
        boolean result = service.canMakeSeatmapRequest(businessUser);
        
        // Assert
        assertTrue(result);
        verify(mockUsageRepository).canMakeSeatmapRequest(testUserId, -1);
    }
    
    @Test
    void recordBookmarkCreation_ProUser_WithinLimit_ShouldSucceed() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockUsageRepository.canCreateBookmark(testUserId, 50)).thenReturn(true);
        doNothing().when(mockUsageRepository).recordBookmarkCreation(testUserId);
        
        // Act & Assert
        assertDoesNotThrow(() -> service.recordBookmarkCreation(proUser));
        verify(mockUsageRepository).canCreateBookmark(testUserId, 50);
        verify(mockUsageRepository).recordBookmarkCreation(testUserId);
    }
    
    @Test
    void recordBookmarkCreation_FreeUser_ShouldThrowException() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User freeUser = createTestUser(AccountTier.FREE);
        when(mockUsageRepository.canCreateBookmark(testUserId, 0)).thenReturn(false);
        when(mockUsageRepository.getCurrentMonthBookmarkCount(testUserId)).thenReturn(0);
        
        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class, 
            () -> service.recordBookmarkCreation(freeUser));
        
        assertTrue(exception.getMessage().contains("Bookmark creation is not available for FREE tier"));
        verify(mockUsageRepository).canCreateBookmark(testUserId, 0);
        verify(mockUsageRepository, never()).recordBookmarkCreation(anyString());
    }
    
    @Test
    void recordBookmarkCreation_ProUser_ExceedsLimit_ShouldThrowException() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockUsageRepository.canCreateBookmark(testUserId, 50)).thenReturn(false);
        when(mockUsageRepository.getCurrentMonthBookmarkCount(testUserId)).thenReturn(50);
        
        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class, 
            () -> service.recordBookmarkCreation(proUser));
        
        assertTrue(exception.getMessage().contains("Monthly bookmark limit reached (50/50)"));
        assertTrue(exception.getMessage().contains("PRO tier"));
        verify(mockUsageRepository).canCreateBookmark(testUserId, 50);
        verify(mockUsageRepository, never()).recordBookmarkCreation(anyString());
    }
    
    @Test
    void recordSeatmapRequest_ProUser_WithinLimit_ShouldSucceed() throws SeatmapException {
        // Arrange - minimal setup, no tier definitions needed for recording
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        doNothing().when(mockUsageRepository).recordSeatmapRequest(testUserId);
        
        // Act & Assert - recordSeatmapRequest should just record usage, no limit checking
        assertDoesNotThrow(() -> service.recordSeatmapRequest(proUser));
        verify(mockUsageRepository).recordSeatmapRequest(testUserId);
    }
    
    @Test
    void recordSeatmapRequest_AnyUser_ShouldRecordUsage() throws SeatmapException {
        // Arrange - minimal setup, no tier definitions needed for recording
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User freeUser = createTestUser(AccountTier.FREE);
        doNothing().when(mockUsageRepository).recordSeatmapRequest(testUserId);
        
        // Act & Assert - recordSeatmapRequest should always succeed when called after successful seat map
        assertDoesNotThrow(() -> service.recordSeatmapRequest(freeUser));
        
        // Verify usage was recorded
        verify(mockUsageRepository).recordSeatmapRequest(testUserId);
    }
    
    @Test
    void getRemainingBookmarks_ProUser_ShouldReturnCorrectCount() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockUsageRepository.getRemainingBookmarks(testUserId, 50)).thenReturn(25);
        
        // Act
        int result = service.getRemainingBookmarks(proUser);
        
        // Assert
        assertEquals(25, result);
        verify(mockUsageRepository).getRemainingBookmarks(testUserId, 50);
    }
    
    @Test
    void getRemainingBookmarks_BusinessUser_ShouldReturnMaxValue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User businessUser = createTestUser(AccountTier.BUSINESS);
        when(mockUsageRepository.getRemainingBookmarks(testUserId, -1)).thenReturn(Integer.MAX_VALUE);
        
        // Act
        int result = service.getRemainingBookmarks(businessUser);
        
        // Assert
        assertEquals(Integer.MAX_VALUE, result);
        verify(mockUsageRepository).getRemainingBookmarks(testUserId, -1);
    }
    
    @Test
    void validateTierTransition_FromBusinessTier_ShouldThrowException() {
        // Create service with no mocked DynamoDB calls (tier transitions don't need tier definitions)
        UserUsageLimitsService simpleService = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        
        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class,
            () -> simpleService.validateTierTransition(AccountTier.BUSINESS, AccountTier.PRO));
        
        assertTrue(exception.getMessage().contains("Business tier cannot be downgraded"));
        assertTrue(exception.getMessage().contains("one-time purchase"));
    }
    
    @Test
    void validateTierTransition_FromFreeToPro_ShouldSucceed() {
        // Create service with no mocked DynamoDB calls (tier transitions don't need tier definitions)
        UserUsageLimitsService simpleService = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        
        // Act & Assert
        assertDoesNotThrow(() -> simpleService.validateTierTransition(AccountTier.FREE, AccountTier.PRO));
    }
    
    @Test
    void validateTierTransition_FromProToFree_ShouldSucceed() {
        // Create service with no mocked DynamoDB calls (tier transitions don't need tier definitions)
        UserUsageLimitsService simpleService = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        
        // Act & Assert
        assertDoesNotThrow(() -> simpleService.validateTierTransition(AccountTier.PRO, AccountTier.FREE));
    }
    
    @Test
    void validateTierTransition_FromProToBusiness_ShouldSucceed() {
        // Create service with no mocked DynamoDB calls (tier transitions don't need tier definitions)
        UserUsageLimitsService simpleService = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        
        // Act & Assert
        assertDoesNotThrow(() -> simpleService.validateTierTransition(AccountTier.PRO, AccountTier.BUSINESS));
    }
    
    @Test
    void validateTierTransition_FromFreeToBusiness_ShouldSucceed() {
        // Create service with no mocked DynamoDB calls (tier transitions don't need tier definitions)
        UserUsageLimitsService simpleService = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        
        // Act & Assert
        assertDoesNotThrow(() -> simpleService.validateTierTransition(AccountTier.FREE, AccountTier.BUSINESS));
    }
    
    @Test
    void service_FailsToLoadTierDefinitions_ShouldDenyAll() {
        // Arrange
        when(mockDynamoDbClient.scan(any(ScanRequest.class)))
            .thenThrow(ResourceNotFoundException.builder().message("Table not found").build());
        
        // Act - Service should initialize but deny all requests
        UserUsageLimitsService failingService = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        
        // Assert - Should throw exception due to tier definitions not loading
        // The service fails at getTierDefinition() before reaching repository calls
        assertThrows(SeatmapException.class, () -> {
            failingService.canCreateBookmark(proUser);
        });
    }
    
    @Test
    void service_HandlesEmptyTierDefinitions_ShouldDenyAll() {
        // Arrange - Mock empty scan response
        ScanResponse emptyScanResponse = ScanResponse.builder().items().build();
        when(mockDynamoDbClient.scan(any(ScanRequest.class))).thenReturn(emptyScanResponse);
        
        // Act - Service should initialize but deny all requests when no tier definitions found
        UserUsageLimitsService emptyService = new UserUsageLimitsService(mockUsageRepository, mockDynamoDbClient);
        User businessUser = createTestUser(AccountTier.BUSINESS);
        
        // Assert - Should throw exception due to empty tier definitions
        // The service fails at getTierDefinition() before reaching repository calls
        assertThrows(SeatmapException.class, () -> {
            emptyService.canCreateBookmark(businessUser);
        });
    }
}