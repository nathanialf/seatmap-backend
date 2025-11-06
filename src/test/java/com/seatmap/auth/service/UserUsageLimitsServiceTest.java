package com.seatmap.auth.service;

import com.seatmap.auth.repository.BookmarkRepository;
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
    private BookmarkRepository mockBookmarkRepository;
    
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
        freeItem.put("canDowngrade", AttributeValue.builder().bool(true).build());
        freeItem.put("publiclyAccessible", AttributeValue.builder().bool(true).build());
        freeItem.put("active", AttributeValue.builder().bool(true).build());
        
        Map<String, AttributeValue> proItem = new HashMap<>();
        proItem.put("tierId", AttributeValue.builder().s("pro-us-2025").build());
        proItem.put("tierName", AttributeValue.builder().s("PRO").build());
        proItem.put("maxBookmarks", AttributeValue.builder().n("10").build());
        proItem.put("maxSeatmapCalls", AttributeValue.builder().n("500").build());
        proItem.put("canDowngrade", AttributeValue.builder().bool(true).build());
        proItem.put("publiclyAccessible", AttributeValue.builder().bool(true).build());
        proItem.put("active", AttributeValue.builder().bool(true).build());
        
        Map<String, AttributeValue> businessItem = new HashMap<>();
        businessItem.put("tierId", AttributeValue.builder().s("business-us-2025").build());
        businessItem.put("tierName", AttributeValue.builder().s("BUSINESS").build());
        businessItem.put("maxBookmarks", AttributeValue.builder().n("-1").build());
        businessItem.put("maxSeatmapCalls", AttributeValue.builder().n("-1").build());
        businessItem.put("canDowngrade", AttributeValue.builder().bool(false).build());
        businessItem.put("publiclyAccessible", AttributeValue.builder().bool(true).build());
        businessItem.put("active", AttributeValue.builder().bool(true).build());
        
        Map<String, AttributeValue> devItem = new HashMap<>();
        devItem.put("tierId", AttributeValue.builder().s("dev-us-2025").build());
        devItem.put("tierName", AttributeValue.builder().s("DEV").build());
        devItem.put("maxBookmarks", AttributeValue.builder().n("-1").build());
        devItem.put("maxSeatmapCalls", AttributeValue.builder().n("-1").build());
        devItem.put("canDowngrade", AttributeValue.builder().bool(false).build());
        devItem.put("publiclyAccessible", AttributeValue.builder().bool(false).build());
        devItem.put("active", AttributeValue.builder().bool(true).build());
        
        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(freeItem, proItem, businessItem, devItem)
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
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User freeUser = createTestUser(AccountTier.FREE);
        when(mockBookmarkRepository.countBookmarksByUserId(testUserId)).thenReturn(0);
        
        // Act
        boolean result = service.canCreateBookmark(freeUser);
        
        // Assert
        assertFalse(result); // FREE tier can't create bookmarks
        verify(mockBookmarkRepository).countBookmarksByUserId(testUserId);
    }
    
    @Test
    void canCreateBookmark_ProUser_WithinLimit_ShouldReturnTrue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockBookmarkRepository.countBookmarksByUserId(testUserId)).thenReturn(5); // Under limit of 10
        
        // Act
        boolean result = service.canCreateBookmark(proUser);
        
        // Assert
        assertTrue(result);
        verify(mockBookmarkRepository).countBookmarksByUserId(testUserId);
    }
    
    @Test
    void canCreateBookmark_ProUser_ExceedsLimit_ShouldReturnFalse() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockBookmarkRepository.countBookmarksByUserId(testUserId)).thenReturn(10); // At limit of 10
        
        // Act
        boolean result = service.canCreateBookmark(proUser);
        
        // Assert
        assertFalse(result);
        verify(mockBookmarkRepository).countBookmarksByUserId(testUserId);
    }
    
    @Test
    void canCreateBookmark_BusinessUser_ShouldReturnTrue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User businessUser = createTestUser(AccountTier.BUSINESS);
        // No need to mock countBookmarksByUserId - unlimited tier doesn't call it
        
        // Act
        boolean result = service.canCreateBookmark(businessUser);
        
        // Assert
        assertTrue(result); // BUSINESS tier has unlimited bookmarks
        // No verify needed - unlimited tier returns true immediately
    }
    
    @Test
    void canCreateBookmark_ExceptionThrown_ShouldReturnFalse() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockBookmarkRepository.countBookmarksByUserId(testUserId))
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
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
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
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
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
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
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
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockBookmarkRepository.countBookmarksByUserId(testUserId)).thenReturn(5); // Under limit
        
        // Act & Assert
        assertDoesNotThrow(() -> service.recordBookmarkCreation(proUser));
        verify(mockBookmarkRepository).countBookmarksByUserId(testUserId);
        // Note: Real-time counting doesn't record to UserUsageRepository anymore
    }
    
    @Test
    void recordBookmarkCreation_FreeUser_ShouldThrowException() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User freeUser = createTestUser(AccountTier.FREE);
        when(mockBookmarkRepository.countBookmarksByUserId(testUserId)).thenReturn(0);
        
        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class, 
            () -> service.recordBookmarkCreation(freeUser));
        
        assertTrue(exception.getMessage().contains("Bookmark creation is not available for FREE tier"));
        verify(mockBookmarkRepository, times(2)).countBookmarksByUserId(testUserId); // Called in canCreateBookmark and for error message
    }
    
    @Test
    void recordBookmarkCreation_ProUser_ExceedsLimit_ShouldThrowException() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockBookmarkRepository.countBookmarksByUserId(testUserId)).thenReturn(10); // At limit
        
        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class, 
            () -> service.recordBookmarkCreation(proUser));
        
        assertTrue(exception.getMessage().contains("Monthly bookmark limit reached (10/10)"));
        assertTrue(exception.getMessage().contains("PRO tier"));
        verify(mockBookmarkRepository, times(2)).countBookmarksByUserId(testUserId); // Called in canCreateBookmark and for error message
    }
    
    @Test
    void recordSeatmapRequest_ProUser_WithinLimit_ShouldSucceed() throws SeatmapException {
        // Arrange - minimal setup, no tier definitions needed for recording
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        doNothing().when(mockUsageRepository).recordSeatmapRequest(testUserId);
        
        // Act & Assert - recordSeatmapRequest should just record usage, no limit checking
        assertDoesNotThrow(() -> service.recordSeatmapRequest(proUser));
        verify(mockUsageRepository).recordSeatmapRequest(testUserId);
    }
    
    @Test
    void recordSeatmapRequest_AnyUser_ShouldRecordUsage() throws SeatmapException {
        // Arrange - minimal setup, no tier definitions needed for recording
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
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
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User proUser = createTestUser(AccountTier.PRO);
        when(mockBookmarkRepository.countBookmarksByUserId(testUserId)).thenReturn(3); // 10 - 3 = 7 remaining
        
        // Act
        int result = service.getRemainingBookmarks(proUser);
        
        // Assert
        assertEquals(7, result); // 10 limit - 3 current = 7 remaining
        verify(mockBookmarkRepository).countBookmarksByUserId(testUserId);
    }
    
    @Test
    void getRemainingBookmarks_BusinessUser_ShouldReturnMaxValue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User businessUser = createTestUser(AccountTier.BUSINESS);
        // No need to mock countBookmarksByUserId - unlimited tier doesn't call it
        
        // Act
        int result = service.getRemainingBookmarks(businessUser);
        
        // Assert
        assertEquals(Integer.MAX_VALUE, result); // Unlimited for BUSINESS tier
        // No verify needed - unlimited tier returns MAX_VALUE immediately
    }
    
    @Test
    void validateTierTransition_FromBusinessTier_ShouldThrowException() {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        
        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class,
            () -> service.validateTierTransition(AccountTier.BUSINESS, AccountTier.PRO));
        
        assertTrue(exception.getMessage().contains("Business tier cannot be downgraded"));
        assertTrue(exception.getMessage().contains("one-time purchase"));
    }
    
    @Test
    void validateTierTransition_FromFreeToPro_ShouldSucceed() {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        
        // Act & Assert
        assertDoesNotThrow(() -> service.validateTierTransition(AccountTier.FREE, AccountTier.PRO));
    }
    
    @Test
    void validateTierTransition_FromProToFree_ShouldSucceed() {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        
        // Act & Assert
        assertDoesNotThrow(() -> service.validateTierTransition(AccountTier.PRO, AccountTier.FREE));
    }
    
    @Test
    void validateTierTransition_FromProToBusiness_ShouldSucceed() {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        
        // Act & Assert
        assertDoesNotThrow(() -> service.validateTierTransition(AccountTier.PRO, AccountTier.BUSINESS));
    }
    
    @Test
    void validateTierTransition_FromFreeToBusiness_ShouldSucceed() {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        
        // Act & Assert
        assertDoesNotThrow(() -> service.validateTierTransition(AccountTier.FREE, AccountTier.BUSINESS));
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
    
    // DEV Tier Tests
    
    @Test
    void canCreateBookmark_DevUser_ShouldReturnTrue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User devUser = createTestUser(AccountTier.DEV);
        // No need to mock countBookmarksByUserId - unlimited tier doesn't call it
        
        // Act
        boolean result = service.canCreateBookmark(devUser);
        
        // Assert
        assertTrue(result); // DEV tier has unlimited bookmarks
        // No verify needed - unlimited tier returns true immediately
    }
    
    @Test
    void canMakeSeatmapRequest_DevUser_ShouldReturnTrue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User devUser = createTestUser(AccountTier.DEV);
        when(mockUsageRepository.canMakeSeatmapRequest(testUserId, -1)).thenReturn(true);
        
        // Act
        boolean result = service.canMakeSeatmapRequest(devUser);
        
        // Assert
        assertTrue(result);
        verify(mockUsageRepository).canMakeSeatmapRequest(testUserId, -1);
    }
    
    @Test
    void recordBookmarkCreation_DevUser_ShouldSucceed() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User devUser = createTestUser(AccountTier.DEV);
        // No need to mock countBookmarksByUserId - unlimited tier doesn't call it
        
        // Act & Assert
        assertDoesNotThrow(() -> service.recordBookmarkCreation(devUser));
        // No verify needed - unlimited tier passes validation immediately
    }
    
    @Test
    void getRemainingBookmarks_DevUser_ShouldReturnMaxValue() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User devUser = createTestUser(AccountTier.DEV);
        // No need to mock countBookmarksByUserId - unlimited tier doesn't call it
        
        // Act
        int result = service.getRemainingBookmarks(devUser);
        
        // Assert
        assertEquals(Integer.MAX_VALUE, result); // Unlimited for DEV tier
        // No verify needed - unlimited tier returns MAX_VALUE immediately
    }
    
    @Test
    void getRemainingBookmarks_DevUser_ShouldReturnMaxValueForSeatmaps() throws SeatmapException {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        User devUser = createTestUser(AccountTier.DEV);
        when(mockUsageRepository.getRemainingSeatmapRequests(testUserId, -1)).thenReturn(Integer.MAX_VALUE);
        
        // Act
        int result = service.getRemainingSeatmapRequests(devUser);
        
        // Assert
        assertEquals(Integer.MAX_VALUE, result);
        verify(mockUsageRepository).getRemainingSeatmapRequests(testUserId, -1);
    }
    
    @Test
    void validateTierTransition_FromDevTier_ShouldThrowException() {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        
        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class,
            () -> service.validateTierTransition(AccountTier.DEV, AccountTier.PRO));
        
        assertTrue(exception.getMessage().contains("Developer tier cannot be downgraded"));
    }
    
    @Test
    void validateTierTransition_ToDevTier_ShouldThrowException() {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        
        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class,
            () -> service.validateTierTransition(AccountTier.FREE, AccountTier.DEV));
        
        assertTrue(exception.getMessage().contains("not publicly accessible"));
    }
    
    @Test
    void validateTierTransition_DevToDevTier_ShouldThrowException() {
        // Arrange
        setupMockTierDefinitions();
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        
        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class,
            () -> service.validateTierTransition(AccountTier.DEV, AccountTier.DEV));
        
        assertTrue(exception.getMessage().contains("Developer tier cannot be downgraded"));
    }
    
    @Test
    void validateTierTransition_ToNonPubliclyAccessibleTier_ShouldThrowException() {
        // Arrange - Create a custom tier definition where PRO is not publicly accessible
        Map<String, AttributeValue> freeItem = new HashMap<>();
        freeItem.put("tierId", AttributeValue.builder().s("free-us-2025").build());
        freeItem.put("tierName", AttributeValue.builder().s("FREE").build());
        freeItem.put("canDowngrade", AttributeValue.builder().bool(true).build());
        freeItem.put("publiclyAccessible", AttributeValue.builder().bool(true).build());
        freeItem.put("active", AttributeValue.builder().bool(true).build());
        
        Map<String, AttributeValue> nonPublicProItem = new HashMap<>();
        nonPublicProItem.put("tierId", AttributeValue.builder().s("pro-us-2025").build());
        nonPublicProItem.put("tierName", AttributeValue.builder().s("PRO").build());
        nonPublicProItem.put("canDowngrade", AttributeValue.builder().bool(true).build());
        nonPublicProItem.put("publiclyAccessible", AttributeValue.builder().bool(false).build());
        nonPublicProItem.put("active", AttributeValue.builder().bool(true).build());
        
        ScanResponse mockScanResponse = ScanResponse.builder()
            .items(freeItem, nonPublicProItem)
            .build();
            
        when(mockDynamoDbClient.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);
        service = new UserUsageLimitsService(mockUsageRepository, mockBookmarkRepository, mockDynamoDbClient);
        
        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class,
            () -> service.validateTierTransition(AccountTier.FREE, AccountTier.PRO));
        
        assertTrue(exception.getMessage().contains("not publicly accessible"));
    }
}