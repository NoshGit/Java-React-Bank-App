# Logging Implementation Guide

## Overview
This banking application uses **Logback** with **SLF4J** for comprehensive, production-ready logging. The configuration supports multiple appenders, async logging, JSON structured logging, and environment-specific configurations.

## Key Features Implemented

### 1. **Multiple Appenders**
- **CONSOLE**: Colored output for development environments
- **FILE**: Rolling file appender for application logs
- **JSON_FILE**: Structured JSON logging for monitoring/ELK stacks
- **ERROR_FILE**: Dedicated error log file

### 2. **Rolling Policies**
All file appenders use size and time-based rolling:
- **Max File Size**: 10MB per log file
- **Max History**: 30 days of logs retained
- **Total Size Cap**: 1GB for all logs combined (500MB for errors)
- **Compression**: Automatic gzip compression of rotated files

### 3. **Async Logging**
Non-blocking async appenders for performance:
- **ASYNC_FILE**: Buffers regular logs (queue size: 512)
- **ASYNC_JSON_FILE**: Buffers JSON logs (queue size: 512)
- **ASYNC_ERROR_FILE**: Buffers error logs (queue size: 256)
- Prevents application thread blocking when writing logs

### 4. **Log Formats**
- **Console**: Colored, human-readable with timestamps and thread info
- **File**: Standard format with full context
- **JSON**: Logstash-compatible JSON format for log aggregation

### 5. **Environment-Specific Configuration**
Profiles control logging behavior:
- **dev (default)**: 
  - Log level: DEBUG
  - Location: `logs/dev/`
  - Detailed output for development
  
- **prod**:
  - Log level: INFO
  - Location: `logs/prod/`
  - Performance-optimized

### 6. **Log Levels per Package**

| Package | Level | Purpose |
|---------|-------|---------|
| `com.example` | DEBUG | Application code |
| `com.example.service` | DEBUG | Service layer details |
| `com.example.controller` | DEBUG | Controller requests |
| `org.springframework` | INFO | Framework logs |
| `org.springframework.security` | DEBUG | Security details |
| `org.hibernate` | INFO | JPA/Hibernate |
| `org.hibernate.SQL` | DEBUG | SQL statements |
| `org.apache.kafka` | INFO | Kafka operations |
| `org.springframework.kafka` | DEBUG | Spring Kafka |
| `oracle.jdbc` | WARN | Database driver |

## Log File Locations

### Development (Default)
```
logs/dev/
├── application.log              # Main application log
├── application-json.log         # Structured JSON log
├── error.log                    # Error-only log
├── application-2026-08-11.*.log.gz     # Rotated files
└── application-json-2026-08-11.*.log.gz
```

### Production
```
logs/prod/
├── application.log              # Main application log
├── application-json.log         # Structured JSON log
├── error.log                    # Error-only log
└── [rotated and compressed files]
```

## Configuration Files

### 1. **logback-spring.xml** (Main Configuration)
Located in `src/main/resources/logback-spring.xml`

Key sections:
- `<springProfile>`: Environment-specific settings
- `<appender>`: Log destinations (console, files, async)
- `<logger>`: Package-specific log levels
- `<root>`: Default logging configuration

### 2. **application.yaml** (Spring Boot Config)
```yaml
spring:
  profiles:
    active: dev  # Change to 'prod' for production

logging:
  level:
    root: INFO
```

### 3. **pom.xml** (Dependencies)
Added dependencies:
- `org.projectlombok:lombok`: For `@Slf4j` annotation
- `net.logstash.logback:logstash-logback-encoder`: For JSON logging

## Using Logging in Code

### Adding @Slf4j Annotation
```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AccountService {
    public void transferFunds(Long accountId) {
        log.info("Transferring funds from account: {}", accountId);
        try {
            // Business logic
            log.debug("Transfer completed successfully");
        } catch (Exception e) {
            log.error("Transfer failed for account: {}", accountId, e);
        }
    }
}
```

### Log Levels Usage

| Level | Usage | Example |
|-------|-------|---------|
| **TRACE** | Very detailed debugging | Variable values during loops |
| **DEBUG** | Detailed application flow | Method entry/exit, variable states |
| **INFO** | Important business events | Account transfers, payments initiated |
| **WARN** | Potentially harmful situations | Insufficient funds, account inactive |
| **ERROR** | Error events that may recoverable | Failed external API calls, validation errors |
| **FATAL** | Very severe errors | System failures |

### Examples from Implementation

#### AccountService
```java
log.info("Fetching accounts for customer: {}", authentication.getName());
log.debug("Retrieved {} accounts for customer: {}", accounts.size(), customerNumber);
log.info("Updating account {} status to: {}", accountId, newStatus);
```

#### MoneyMovementService (Transfer)
```java
log.info("Transfer initiated from account {} to account {} amount: {} by user: {}", 
        accountId, toAccountId, amount, authentication.getName());
log.warn("Transfer failed - insufficient funds in account {}", accountId);
log.error("Transfer denied - accounts do not belong to caller");
log.info("Transfer completed successfully...");
```

#### TransactionStatsPublisher (Kafka)
```java
log.info("Publishing completed transaction {} to Kafka topic: {}", txn.getTxnId(), topic);
log.debug("Transaction {} successfully published to Kafka after commit", txn.getTxnId());
```

## Performance Considerations

1. **Async Logging**: All file operations are non-blocking
2. **Lazy Evaluation**: Use `{}` placeholders instead of string concatenation
   - ✅ `log.info("User: {}", user.getName())`
   - ❌ `log.info("User: " + user.getName())`

3. **Queue Management**: Async queues automatically discard low-priority logs when full (production)

4. **File Rotation**: Automatic compression prevents disk space issues

## Monitoring & Analysis

### Real-time Monitoring
```bash
# View latest logs
tail -f logs/dev/application.log

# View errors only
tail -f logs/dev/error.log

# Search for specific user activity
grep "customer: [CUSTOMER_ID]" logs/dev/application.log
```

### JSON Logs for ELK Stack
Logstash-compatible JSON format in `application-json.log`:
```json
{
  "@timestamp": "2026-08-11T02:13:58.123Z",
  "@version": 1,
  "message": "Transfer initiated...",
  "logger_name": "com.example.bankapi.service.MoneyMovementService",
  "level": "INFO",
  "application": "bankapi",
  "environment": "dev"
}
```

## Changing Profiles

### Development (Default)
```bash
mvn spring-boot:run
# or
java -jar application.jar
```

### Production
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
# or
java -jar application.jar --spring.profiles.active=prod
```

## Best Practices

1. **Use appropriate log levels**
   - INFO for business transactions
   - DEBUG for technical details
   - WARN for issues that don't stop execution
   - ERROR for exceptions and failures

2. **Include context**
   - Always log user/customer IDs with actions
   - Include request/transaction IDs for tracing
   - Log entry and exit for critical methods

3. **Avoid logging sensitive data**
   - Never log passwords or tokens
   - Be cautious with account numbers in production
   - Consider PII (Personally Identifiable Information)

4. **Monitor log file sizes**
   - Check `logs/` directory periodically
   - Archive old logs if needed
   - Total cap is 1GB per environment

## Troubleshooting

### No logs appearing
1. Check that `logback-spring.xml` is in `src/main/resources/`
2. Verify Maven compiled the resource
3. Check the configured log directory has write permissions

### Logs accumulating too much
1. Review the rolling policy settings
2. Increase `maxHistory` or `totalSizeCap` as needed
3. Add a cleanup job to archive old logs

### Performance issues
1. Check async queue depth in Logback config
2. Verify file I/O isn't blocking the event loop
3. Consider reducing DEBUG level to INFO in production

## Additional Resources

- [Logback Documentation](http://logback.qos.ch/)
- [SLF4J Documentation](https://www.slf4j.org/)
- [Lombok @Slf4j Documentation](https://projectlombok.org/features/log)
