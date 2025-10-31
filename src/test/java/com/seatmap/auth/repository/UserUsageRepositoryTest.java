package com.seatmap.auth.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.UserUsageHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUsageRepositoryTest {
    
    @Mock
    private DynamoDbClient mockDynamoDbClient;
    
    private UserUsageRepository repository;
    private final String testUserId = "test-user-123";
    private final String testMonth = "2024-10";
    
    @BeforeEach
    void setUp() {
        repository = new UserUsageRepository(mockDynamoDbClient);
    }
    
    private Map<String, AttributeValue> createMockDynamoItem(UserUsageHistory history) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(history.getUserId()).build());
        item.put("monthYear", AttributeValue.builder().s(history.getMonthYear()).build());
        item.put("bookmarksCreated", AttributeValue.builder().n(history.getBookmarksCreated().toString()).build());
        item.put("seatmapRequestsUsed", AttributeValue.builder().n(history.getSeatmapRequestsUsed().toString()).build());
        return item;
    }
    
    private UserUsageHistory createTestUsageHistory() {
        UserUsageHistory history = new UserUsageHistory(testUserId, testMonth);
        history.setBookmarksCreated(5);
        history.setSeatmapRequestsUsed(10);
        return history;
    }
    
    @Test
    void findByUserIdAndMonth_ExistingRecord_ShouldReturnUsageHistory() {
        // Arrange
        UserUsageHistory expectedHistory = createTestUsageHistory();
        Map<String, AttributeValue> mockItem = createMockDynamoItem(expectedHistory);
        
        GetItemResponse mockResponse = GetItemResponse.builder()
            .item(mockItem)
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        Optional<UserUsageHistory> result = repository.findByUserIdAndMonth(testUserId, testMonth);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUserId, result.get().getUserId());
        assertEquals(testMonth, result.get().getMonthYear());
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void findByUserIdAndMonth_NonExistingRecord_ShouldReturnEmpty() {
        // Arrange
        GetItemResponse mockResponse = GetItemResponse.builder().build(); // No item
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        Optional<UserUsageHistory> result = repository.findByUserIdAndMonth(testUserId, testMonth);
        
        // Assert
        assertFalse(result.isPresent());
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void findByUserIdAndMonth_DynamoException_ShouldReturnEmpty() {
        // Arrange
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
            .thenThrow(ResourceNotFoundException.builder().message("Table not found").build());
        
        // Act
        Optional<UserUsageHistory> result = repository.findByUserIdAndMonth(testUserId, testMonth);
        
        // Assert
        assertFalse(result.isPresent());
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void findCurrentMonthUsage_ShouldCallFindByUserIdAndMonth() {
        // Arrange
        UserUsageHistory expectedHistory = new UserUsageHistory(testUserId);
        Map<String, AttributeValue> mockItem = createMockDynamoItem(expectedHistory);
        
        GetItemResponse mockResponse = GetItemResponse.builder()
            .item(mockItem)
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        Optional<UserUsageHistory> result = repository.findCurrentMonthUsage(testUserId);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUserId, result.get().getUserId());
        assertEquals(UserUsageHistory.getCurrentMonthYear(), result.get().getMonthYear());
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void save_ValidUsageHistory_ShouldCallPutItem() throws SeatmapException {
        // Arrange
        UserUsageHistory history = createTestUsageHistory();
        when(mockDynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());
        
        // Act
        repository.save(history);
        
        // Assert
        verify(mockDynamoDbClient).putItem(any(PutItemRequest.class));
    }
    
    @Test
    void save_DynamoException_ShouldThrowSeatmapException() {
        // Arrange
        UserUsageHistory history = createTestUsageHistory();
        when(mockDynamoDbClient.putItem(any(PutItemRequest.class)))
            .thenThrow(ResourceNotFoundException.builder().message("Table not found").build());
        
        // Act & Assert
        assertThrows(SeatmapException.class, () -> repository.save(history));
        verify(mockDynamoDbClient).putItem(any(PutItemRequest.class));
    }
    
    @Test
    void getOrCreateCurrentMonth_ExistingRecord_ShouldReturnExisting() {
        // Arrange
        UserUsageHistory expectedHistory = new UserUsageHistory(testUserId);
        expectedHistory.setBookmarksCreated(5);
        Map<String, AttributeValue> mockItem = createMockDynamoItem(expectedHistory);
        
        GetItemResponse mockResponse = GetItemResponse.builder()
            .item(mockItem)
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        UserUsageHistory result = repository.getOrCreateCurrentMonth(testUserId);
        
        // Assert
        assertEquals(testUserId, result.getUserId());
        assertEquals(Integer.valueOf(5), result.getBookmarksCreated());
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void getOrCreateCurrentMonth_NonExistingRecord_ShouldCreateNew() {
        // Arrange
        GetItemResponse mockResponse = GetItemResponse.builder().build(); // No item
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        UserUsageHistory result = repository.getOrCreateCurrentMonth(testUserId);
        
        // Assert
        assertEquals(testUserId, result.getUserId());
        assertEquals(UserUsageHistory.getCurrentMonthYear(), result.getMonthYear());
        assertEquals(Integer.valueOf(0), result.getBookmarksCreated());
        assertEquals(Integer.valueOf(0), result.getSeatmapRequestsUsed());
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void recordBookmarkCreation_ShouldIncrementAndSave() throws SeatmapException {
        // Arrange
        GetItemResponse mockResponse = GetItemResponse.builder().build(); // No existing item
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        when(mockDynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());
        
        // Act
        repository.recordBookmarkCreation(testUserId);
        
        // Assert
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
        verify(mockDynamoDbClient).putItem(any(PutItemRequest.class));
    }
    
    @Test
    void recordSeatmapRequest_ShouldIncrementAndSave() throws SeatmapException {
        // Arrange
        GetItemResponse mockResponse = GetItemResponse.builder().build(); // No existing item
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        when(mockDynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());
        
        // Act
        repository.recordSeatmapRequest(testUserId);
        
        // Assert
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
        verify(mockDynamoDbClient).putItem(any(PutItemRequest.class));
    }
    
    @Test
    void getCurrentMonthBookmarkCount_ExistingRecord_ShouldReturnCount() {
        // Arrange
        UserUsageHistory expectedHistory = new UserUsageHistory(testUserId);
        expectedHistory.setBookmarksCreated(7);
        Map<String, AttributeValue> mockItem = createMockDynamoItem(expectedHistory);
        
        GetItemResponse mockResponse = GetItemResponse.builder()
            .item(mockItem)
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        int result = repository.getCurrentMonthBookmarkCount(testUserId);
        
        // Assert
        assertEquals(7, result);
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void getCurrentMonthBookmarkCount_NoRecord_ShouldReturnZero() {
        // Arrange
        GetItemResponse mockResponse = GetItemResponse.builder().build(); // No item
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        int result = repository.getCurrentMonthBookmarkCount(testUserId);
        
        // Assert
        assertEquals(0, result);
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void getCurrentMonthSeatmapCount_ExistingRecord_ShouldReturnCount() {
        // Arrange
        UserUsageHistory expectedHistory = new UserUsageHistory(testUserId);
        expectedHistory.setSeatmapRequestsUsed(15);
        Map<String, AttributeValue> mockItem = createMockDynamoItem(expectedHistory);
        
        GetItemResponse mockResponse = GetItemResponse.builder()
            .item(mockItem)
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        int result = repository.getCurrentMonthSeatmapCount(testUserId);
        
        // Assert
        assertEquals(15, result);
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void canCreateBookmark_WithinLimit_ShouldReturnTrue() {
        // Arrange
        UserUsageHistory expectedHistory = new UserUsageHistory(testUserId);
        expectedHistory.setBookmarksCreated(5);
        Map<String, AttributeValue> mockItem = createMockDynamoItem(expectedHistory);
        
        GetItemResponse mockResponse = GetItemResponse.builder()
            .item(mockItem)
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        boolean result = repository.canCreateBookmark(testUserId, 10);
        
        // Assert
        assertTrue(result);
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void canCreateBookmark_ExceedsLimit_ShouldReturnFalse() {
        // Arrange
        UserUsageHistory expectedHistory = new UserUsageHistory(testUserId);
        expectedHistory.setBookmarksCreated(10);
        Map<String, AttributeValue> mockItem = createMockDynamoItem(expectedHistory);
        
        GetItemResponse mockResponse = GetItemResponse.builder()
            .item(mockItem)
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        boolean result = repository.canCreateBookmark(testUserId, 10);
        
        // Assert
        assertFalse(result);
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void canCreateBookmark_UnlimitedTier_ShouldReturnTrue() {
        // Arrange
        UserUsageHistory expectedHistory = new UserUsageHistory(testUserId);
        expectedHistory.setBookmarksCreated(1000);
        Map<String, AttributeValue> mockItem = createMockDynamoItem(expectedHistory);
        
        GetItemResponse mockResponse = GetItemResponse.builder()
            .item(mockItem)
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        boolean result = repository.canCreateBookmark(testUserId, -1); // Unlimited
        
        // Assert
        assertTrue(result);
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void canMakeSeatmapRequest_WithinLimit_ShouldReturnTrue() {
        // Arrange
        UserUsageHistory expectedHistory = new UserUsageHistory(testUserId);
        expectedHistory.setSeatmapRequestsUsed(50);
        Map<String, AttributeValue> mockItem = createMockDynamoItem(expectedHistory);
        
        GetItemResponse mockResponse = GetItemResponse.builder()
            .item(mockItem)
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        boolean result = repository.canMakeSeatmapRequest(testUserId, 100);
        
        // Assert
        assertTrue(result);
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void getRemainingBookmarks_ShouldReturnCorrectCount() {
        // Arrange
        UserUsageHistory expectedHistory = new UserUsageHistory(testUserId);
        expectedHistory.setBookmarksCreated(3);
        Map<String, AttributeValue> mockItem = createMockDynamoItem(expectedHistory);
        
        GetItemResponse mockResponse = GetItemResponse.builder()
            .item(mockItem)
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        int result = repository.getRemainingBookmarks(testUserId, 10);
        
        // Assert
        assertEquals(7, result);
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
    
    @Test
    void getRemainingBookmarks_UnlimitedTier_ShouldReturnMaxValue() {
        // Arrange
        UserUsageHistory expectedHistory = new UserUsageHistory(testUserId);
        expectedHistory.setBookmarksCreated(100);
        Map<String, AttributeValue> mockItem = createMockDynamoItem(expectedHistory);
        
        GetItemResponse mockResponse = GetItemResponse.builder()
            .item(mockItem)
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);
        
        // Act
        int result = repository.getRemainingBookmarks(testUserId, -1); // Unlimited
        
        // Assert
        assertEquals(Integer.MAX_VALUE, result);
        verify(mockDynamoDbClient).getItem(any(GetItemRequest.class));
    }
}