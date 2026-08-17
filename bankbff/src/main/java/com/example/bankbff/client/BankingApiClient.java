package com.example.bankbff.client;

import com.example.bankbff.dto.AccountDto;
import com.example.bankbff.dto.AccountStatusUpdateDto;
import com.example.bankbff.dto.AmountRequestDto;
import com.example.bankbff.dto.PaymentRequestDto;
import com.example.bankbff.dto.TransactionDto;
import com.example.bankbff.dto.TransactionReportLineDto;
import com.example.bankbff.dto.TransferRequestDto;
import com.example.bankbff.dto.TransferResultDto;
import com.example.bankbff.exception.DownstreamApiException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/** Thin passthrough to bankapi. Every call routes errors through toDownstreamException so the
 *  real status code and body reach the caller unchanged (see GlobalExceptionHandler). */
@Component
public class BankingApiClient {

    private final WebClient bankApiWebClient;

    public BankingApiClient(WebClient bankApiWebClient) {
        this.bankApiWebClient = bankApiWebClient;
    }

    public List<AccountDto> getMyAccounts() {
        return bankApiWebClient.get()
                .uri("/api/accounts")
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toDownstreamException)
                .bodyToMono(new ParameterizedTypeReference<List<AccountDto>>() {})
                .block();
    }

    public List<AccountDto> getAccountsForCustomer(String customerNumber) {
        return bankApiWebClient.get()
                .uri("/api/customers/{customerNumber}/accounts", customerNumber)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toDownstreamException)
                .bodyToMono(new ParameterizedTypeReference<List<AccountDto>>() {})
                .block();
    }

    public AccountDto getAccount(Long accountId) {
        return bankApiWebClient.get()
                .uri("/api/accounts/{accountId}", accountId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toDownstreamException)
                .bodyToMono(AccountDto.class)
                .block();
    }

    public List<TransactionDto> getTransactions(Long accountId) {
        return bankApiWebClient.get()
                .uri("/api/accounts/{accountId}/transactions", accountId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toDownstreamException)
                .bodyToMono(new ParameterizedTypeReference<List<TransactionDto>>() {})
                .block();
    }

    public TransferResultDto transfer(Long accountId, TransferRequestDto request) {
        return bankApiWebClient.post()
                .uri("/api/accounts/{accountId}/transfers", accountId)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toDownstreamException)
                .bodyToMono(TransferResultDto.class)
                .block();
    }

    public TransferResultDto payment(Long accountId, PaymentRequestDto request) {
        return bankApiWebClient.post()
                .uri("/api/accounts/{accountId}/payments", accountId)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toDownstreamException)
                .bodyToMono(TransferResultDto.class)
                .block();
    }

    public TransferResultDto deposit(Long accountId, AmountRequestDto request) {
        return bankApiWebClient.post()
                .uri("/api/accounts/{accountId}/deposits", accountId)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toDownstreamException)
                .bodyToMono(TransferResultDto.class)
                .block();
    }

    public TransferResultDto withdrawal(Long accountId, AmountRequestDto request) {
        return bankApiWebClient.post()
                .uri("/api/accounts/{accountId}/withdrawals", accountId)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toDownstreamException)
                .bodyToMono(TransferResultDto.class)
                .block();
    }

    public AccountDto updateStatus(Long accountId, AccountStatusUpdateDto request) {
        return bankApiWebClient.put()
                .uri("/api/accounts/{accountId}/status", accountId)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toDownstreamException)
                .bodyToMono(AccountDto.class)
                .block();
    }

    public List<TransactionReportLineDto> getTransactionReport() {
        return bankApiWebClient.get()
                .uri("/api/reports/transactions")
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toDownstreamException)
                .bodyToMono(new ParameterizedTypeReference<List<TransactionReportLineDto>>() {})
                .block();
    }

    private Mono<? extends Throwable> toDownstreamException(
            org.springframework.web.reactive.function.client.ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new DownstreamApiException(response.statusCode(), body));
    }
}
