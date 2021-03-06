package com.hcl.capstoneserver.invoice;

import com.hcl.capstoneserver.invoice.dto.*;
import com.hcl.capstoneserver.invoice.entities.Invoice;
import com.hcl.capstoneserver.invoice.repositories.InvoiceRepository;
import com.hcl.capstoneserver.user.UserTestUtils;
import com.hcl.capstoneserver.user.dto.ClientDTO;
import com.hcl.capstoneserver.user.dto.SupplierDTO;
import com.hcl.capstoneserver.user.repositories.ClientRepository;
import com.hcl.capstoneserver.user.repositories.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class InvoiceServiceTest {
    @Autowired
    InvoiceService invoiceService;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    UserTestUtils userTestUtils;

    @Autowired
    InvoiceTestUtils invoiceTestUtils;

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    ClientRepository clientRepository;

    List<ClientViewInvoiceDTO> createInvoice; // invoiceNumber : 1234567898, 1234567899
    List<SupplierDTO> suppliers;
    List<ClientDTO> clients;
    Invoice expiredInvoice;

    @Test
    void contextLoads() {
        assertNotNull(invoiceService);
        assertNotNull(userTestUtils);
        assertNotNull(invoiceTestUtils);
    }

    @BeforeEach
    public void beforeEach() {
        invoiceRepository.deleteAll();
        supplierRepository.deleteAll();
        clientRepository.deleteAll();

        suppliers = userTestUtils.createASupplier();
        clients = userTestUtils.createAClient();
        createInvoice = invoiceTestUtils.createInvoice(suppliers);
        expiredInvoice = invoiceTestUtils.createExpiredInvoice(suppliers, clients);
    }

    private InvoiceStatus updateInvoiceStatus(InvoiceStatus status, Integer invoiceId) {
        return invoiceService.statusUpdate(new StatusUpdateInvoiceDTO(
                invoiceId,
                status
        ), "BANK").getStatus();
    }

    @Nested
    @DisplayName("invoice create test")
    class InvoiceCreateTests {
        @Test
        @DisplayName("it should create new invoice")
        public void shouldCreateNewInvoice() {
            assertNotNull(invoiceService.createInvoice(
                    new CreateInvoiceDTO(
                            suppliers.get(0).getSupplierId(),
                            "1234567891",
                            LocalDate.now(),
                            25000.0,
                            CurrencyType.USD
                    ), "client"));
        }

        @Test
        @DisplayName("it should not create new invoice with exists invoice number")
        public void shouldNotCreateNewInvoiceWithSameInvoiceNumber() {
            assertThrows(
                    HttpClientErrorException.class, () ->
                            invoiceService.createInvoice(
                                    new CreateInvoiceDTO(
                                            suppliers.get(0).getSupplierId(),
                                            "1234567898",
                                            LocalDate.now(),
                                            25000.0,
                                            CurrencyType.USD
                                    ), "client"), "400 An invoice number already exists for this supplier."
            );
        }

        @Test
        @DisplayName("it should not create new invoice with old date")
        public void shouldNotCreateNewInvoiceWithOldDate() {
            assertEquals(
                    "400 The invoice date is an older date.",
                    assertThrows(
                            HttpClientErrorException.class,
                            () -> invoiceService.createInvoice(
                                    new CreateInvoiceDTO(
                                            suppliers.get(0).getSupplierId(),
                                            "1234567892",
                                            LocalDate.parse("2021-04-05"),
                                            25000.0,
                                            CurrencyType.USD
                                    ), "client")
                    ).getMessage()
            );
        }

        @Test
        @DisplayName("it should not create new invoice with not exists supplier")
        public void shouldNotCreateNewInvoiceWithNotExistsSupplier() {
            assertEquals(
                    "400 This SUPPLIER is not exist.",
                    assertThrows(
                            HttpClientErrorException.class,
                            () -> invoiceService.createInvoice(
                                    new CreateInvoiceDTO(
                                            "SP_1",
                                            "1234567893",
                                            LocalDate.now(),
                                            25000.0,
                                            CurrencyType.USD
                                    ), "client")
                    ).getMessage()
            );
        }
    }

    @Nested
    @DisplayName("invoice update test")
    class InvoiceUpdateTests {

        // Bank
        // One feature needs to be tested when BANK user is created: invoice status can update only by BANK
        @Nested
        @DisplayName("invoice update test: BANK")
        class InvoiceUpdateBankTests {


            @Test
            @DisplayName("it should update the invoice status")
            public void shouldUpdateInvoiceStatus() {
                assertEquals(
                        InvoiceStatus.IN_REVIEW,
                        updateInvoiceStatus(InvoiceStatus.IN_REVIEW, createInvoice.get(0).getInvoiceId())
                );
            }

            @Test
            @DisplayName("it should not update when invoice is expired")
            public void shouldNotUpdateInvoiceWhenInvoiceIsExpired() {
                assertEquals(
                        "400 You can not update the invoice status, because invoice is expire.",
                        assertThrows(
                                HttpClientErrorException.class, () ->
                                        updateInvoiceStatus(InvoiceStatus.IN_REVIEW, expiredInvoice.getInvoiceId())
                        ).getMessage()
                );
            }

            @Test
            @DisplayName("it should not update when invoice status is REJECTED")
            public void shouldNotUpdateInvoiceWhenStatusIsRejected() {
                updateInvoiceStatus(InvoiceStatus.REJECTED, createInvoice.get(0).getInvoiceId());
                assertEquals(
                        "400 This invoice can not update, because invoice is REJECTED.",
                        assertThrows(
                                HttpClientErrorException.class, () ->
                                        updateInvoiceStatus(
                                                InvoiceStatus.IN_REVIEW,
                                                createInvoice.get(0).getInvoiceId()
                                        )
                        ).getMessage()
                );
            }
        }

        //Client
        @Nested
        @DisplayName("invoice update test: CLIENT")
        class InvoiceUpdateClientTests {
            @Test
            @DisplayName("it should update invoice")
            public void shouldUpdateInvoice() {
                UpdateInvoiceDTO dto = new UpdateInvoiceDTO();
                dto.setInvoiceId(createInvoice.get(0).getInvoiceId());
                dto.setInvoiceNumber("1234567894");
                assertEquals("1234567894", invoiceService.updateInvoice(dto, "client").getInvoiceNumber());
            }

            @Test
            @DisplayName("it should not update invoice when that invoice owner is not a same client")
            public void shouldNotUpdateInvoiceWhenInvoiceOwnerIsNotEqual() {
                assertEquals(
                        "400 client you do not have permission to update this invoice.",
                        assertThrows(
                                HttpClientErrorException.class,
                                () -> invoiceService.updateInvoice(new UpdateInvoiceDTO(
                                        createInvoice.get(1).getInvoiceId(),
                                        suppliers.get(0).getSupplierId(),
                                        "1234567898",
                                        LocalDate.now(),
                                        25000.0,
                                        CurrencyType.USD
                                ), "client")
                        ).getMessage()
                );
            }

            @Test
            @DisplayName("it should not update invoice with old date")
            public void shouldNotUpdateInvoiceWithOldDate() {
                assertEquals(
                        "400 The invoice date is an older date.",
                        assertThrows(
                                HttpClientErrorException.class,
                                () -> invoiceService.updateInvoice(
                                        new UpdateInvoiceDTO(
                                                createInvoice.get(0).getInvoiceId(),
                                                suppliers.get(0).getSupplierId(),
                                                "1234567892",
                                                LocalDate.parse("2021-04-05"),
                                                25000.0,
                                                CurrencyType.USD
                                        ), "client")
                        ).getMessage()
                );
            }

            @Test
            @DisplayName("it should not update invoice with invoice status In_Review, Approved and Rejected")
            public void shouldNotUpdateInvoiceWithInReviewAndApprovedAndRejected() {
                updateInvoiceStatus(InvoiceStatus.IN_REVIEW, createInvoice.get(0).getInvoiceId());
                assertEquals(
                        "400 This invoice can not update, because invoice is IN_REVIEW.",
                        assertThrows(
                                HttpClientErrorException.class,
                                () -> invoiceService.updateInvoice(
                                        new UpdateInvoiceDTO(
                                                createInvoice.get(0).getInvoiceId(),
                                                suppliers.get(0).getSupplierId(),
                                                "1234567892",
                                                LocalDate.now(),
                                                25000.0,
                                                CurrencyType.EUR
                                        ), "client")
                        ).getMessage()
                );
            }
        }
    }

    @Nested
    @DisplayName("invoice delete test")
    class InvoiceDeleteTest {
        @Test
        @DisplayName("it should delete invoice")
        public void shouldDeleteInvoice() {
            //            assertEquals(2, invoiceService.deleteInvoice(createInvoice.get(0).getInvoiceId(), "client"));
        }

        // Invoice can delete client only, suppliers and bank can not delete
        // This test checks the above point and below point (Display Name mentioned thing)
        @Test
        @DisplayName("it should not delete invoice when that invoice owner is not a same client")
        public void shouldNotDeleteInvoiceWhenInvoiceOwnerIsNotEqual() {
            assertEquals(
                    "400 client2 you do not have permission to delete this invoice.",
                    assertThrows(
                            HttpClientErrorException.class,
                            () -> invoiceService.deleteInvoice(createInvoice.get(0).getInvoiceId(), "client2")
                    ).getMessage()
            );
        }

        @Test
        @DisplayName("it should not delete invoice with invoice status In_Review, Approved and Rejected")
        public void shouldNotDeleteInvoiceWithInReviewAndApprovedAndRejected() {
            updateInvoiceStatus(InvoiceStatus.IN_REVIEW, createInvoice.get(0).getInvoiceId());
            assertEquals(
                    "400 This invoice can not delete, because invoice is IN_REVIEW.",
                    assertThrows(
                            HttpClientErrorException.class,
                            () -> invoiceService.deleteInvoice(createInvoice.get(0).getInvoiceId(), "client")
                    ).getMessage()
            );
        }
    }

    @Nested
    @DisplayName("invoice retrieve test")
    class InvoiceRetrieveTest {

        // BANK
        @Nested
        @DisplayName("invoice retrieve test: BANK")
        class InvoiceRetrieveBank {
            @Test
            @DisplayName("it should return all invoice")
            public void shouldReturnAllInvoice() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                assertEquals(3, invoiceService.getBankInvoice(dto, "BANK").getNumberOfElements());
            }

            @Test
            @DisplayName("it should return all invoice By clientId")
            public void shouldReturnAllInvoiceByClientId() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setClientId("CL_00001");
                invoiceService.getBankInvoice(dto, "BANK")
                              .getContent()
                              .forEach(i -> assertEquals("CL_00001", i.getClient().getClientId()));
            }

            @Test
            @DisplayName("it should return all invoice By supplierId")
            public void shouldReturnAllInvoiceBySupplierId() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setSupplierId("SP_00001");

                invoiceService.getBankInvoice(dto, "BANK")
                              .getContent()
                              .forEach(i -> assertEquals("SP_00001", i.getSupplier().getSupplierId()));

            }

            @Test
            @DisplayName("it should return all invoice By invoiceNumber")
            public void shouldReturnAllInvoiceByInvoiceNumber() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setInvoiceNumber("1234567898");

                invoiceService.getBankInvoice(dto, "BANK")
                              .getContent()
                              .forEach(i -> assertEquals("1234567898", i.getInvoiceNumber()));

            }

            @Test
            @DisplayName("it should return all invoice By dateFrom")
            public void shouldReturnAllInvoiceByDateFrom() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setDateFrom(LocalDate.parse("2021-05-01"));

                invoiceService.getBankInvoice(dto, "BANK")
                              .getContent()
                              .forEach(i -> assertEquals(LocalDate.parse("2021-05-01"), i.getInvoiceDate()));

            }

            @Test
            @DisplayName("it should return all invoice Between to date")
            public void shouldReturnAllInvoiceBetweenDateFromAndDateTo() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setDateFrom(LocalDate.parse("2021-05-01"));
                dto.setDateTo(LocalDate.now());

                invoiceService.getBankInvoice(dto, "BANK")
                              .getContent()
                              .forEach(i -> assertThat(
                                      i.getInvoiceDate()
                              ).isIn(LocalDate.parse("2021-05-01"), LocalDate.now()));

            }

            @Test
            @DisplayName("it should return all invoice By ageing")
            public void shouldReturnAllInvoiceByAgeing() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setAgeing(ChronoUnit.DAYS.between(LocalDate.parse("2021-05-01"), LocalDate.now()));

                invoiceService.getBankInvoice(dto, "BANK")
                              .getContent()
                              .forEach(i -> assertEquals(
                                      LocalDate.parse("2021-05-01"),
                                      i.getInvoiceDate()
                              ));
            }

            @Test
            @DisplayName("it should return all invoice By status")
            public void shouldReturnAllInvoiceByStatus() {
                List<InvoiceStatus> statuses = new ArrayList<>();
                statuses.add(InvoiceStatus.IN_REVIEW);
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setStatus(statuses);

                invoiceService.getBankInvoice(dto, "BANK")
                              .getContent()
                              .forEach(i -> assertEquals(
                                      InvoiceStatus.IN_REVIEW,
                                      i.getStatus()
                              ));
            }

            @Test
            @DisplayName("it should return all invoice By currencyType")
            public void shouldReturnAllInvoiceByCurrencyType() {
                List<CurrencyType> currencyTypes = new ArrayList<>();
                currencyTypes.add(CurrencyType.GBP);
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setCurrencyType(currencyTypes);

                assertEquals(1, invoiceService.getBankInvoice(dto, "BANK")
                                              .getNumberOfElements());
            }
        }

        // CLIENT
        @Nested
        @DisplayName("invoice retrieve test: CLIENT")
        class InvoiceRetrieveClient {

            private List<InvoiceStatus> _getStatusList() {
                List<InvoiceStatus> statuses = new ArrayList<>();
                statuses.add(InvoiceStatus.IN_REVIEW);
                return statuses;
            }

            @Test
            @DisplayName("it should return all invoice")
            public void shouldReturnAllInvoice() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                assertEquals(2, invoiceService.getClientInvoice(dto, "client").getNumberOfElements());
            }

            @Test
            @DisplayName("it should return all invoice By supplierId and status")
            public void shouldReturnAllInvoiceBySupplierId() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setSupplierId("SP_00001");
                dto.setStatus(_getStatusList());

                invoiceService.getClientInvoice(dto, "client")
                              .getContent()
                              .forEach(i -> assertEquals("SP_00001", i.getSupplier().getSupplierId()));

            }

            @Test
            @DisplayName("it should return all invoice By invoiceNumber")
            public void shouldReturnAllInvoiceByInvoiceNumber() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setInvoiceNumber("1234567898");
                dto.setDateFrom(LocalDate.now());

                invoiceService.getClientInvoice(dto, "client")
                              .getContent()
                              .forEach(i -> assertEquals("1234567898", i.getInvoiceNumber()));

            }

            @Test
            @DisplayName("it should return all invoice By dateFrom")
            public void shouldReturnAllInvoiceByDateFrom() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setDateFrom(LocalDate.parse("2021-05-01"));

                invoiceService.getClientInvoice(dto, "client")
                              .getContent()
                              .forEach(i -> assertEquals(LocalDate.parse("2021-05-01"), i.getInvoiceDate()));

            }

            @Test
            @DisplayName("it should return all invoice Between to date")
            public void shouldReturnAllInvoiceBetweenDateFromAndDateTo() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setDateFrom(LocalDate.parse("2021-05-01"));
                dto.setDateTo(LocalDate.now());

                invoiceService.getClientInvoice(dto, "client")
                              .getContent()
                              .forEach(i -> assertThat(
                                      i.getInvoiceDate()
                              ).isIn(LocalDate.parse("2021-05-01"), LocalDate.now()));

            }

            @Test
            @DisplayName("it should return all invoice By ageing")
            public void shouldReturnAllInvoiceByAgeing() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setAgeing(ChronoUnit.DAYS.between(LocalDate.parse("2021-05-01"), LocalDate.now()));

                invoiceService.getClientInvoice(dto, "client")
                              .getContent()
                              .forEach(i -> assertEquals(
                                      LocalDate.parse("2021-05-01"),
                                      i.getInvoiceDate()
                              ));
            }

            @Test
            @DisplayName("it should return all invoice By status")
            public void shouldReturnAllInvoiceByStatus() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setStatus(_getStatusList());

                invoiceService.getClientInvoice(dto, "client")
                              .getContent()
                              .forEach(i -> assertEquals(
                                      InvoiceStatus.IN_REVIEW,
                                      i.getStatus()
                              ));
            }

            @Test
            @DisplayName("it should return all invoice By currencyType")
            public void shouldReturnAllInvoiceByCurrencyType() {
                List<CurrencyType> currencyTypes = new ArrayList<>();
                currencyTypes.add(CurrencyType.GBP);
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setCurrencyType(currencyTypes);

                assertEquals(1, invoiceService.getClientInvoice(dto, "client")
                                              .getNumberOfElements());
            }
        }

        //SUPPLIER
        @Nested
        @DisplayName("invoice retrieve test: SUPPLIER")
        class InvoiceRetrieveSupplier {

            private List<InvoiceStatus> _getStatusList() {
                List<InvoiceStatus> statuses = new ArrayList<>();
                statuses.add(InvoiceStatus.IN_REVIEW);
                return statuses;
            }

            @Test
            @DisplayName("it should return all invoice")
            public void shouldReturnAllInvoice() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                assertEquals(2, invoiceService.getSupplierInvoice(dto, "supplier").getNumberOfElements());
            }

            @Test
            @DisplayName("it should return all invoice By clientId and supplierId")
            public void shouldReturnAllInvoiceByClientId() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setClientId("CL_00001");
                invoiceService.getSupplierInvoice(dto, "supplier")
                              .getContent()
                              .forEach(i -> assertEquals("CL_00001", i.getClient().getClientId()));
            }

            @Test
            @DisplayName("it should return all invoice By invoiceNumber")
            public void shouldReturnAllInvoiceByInvoiceNumber() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setInvoiceNumber("1234567898");
                dto.setDateFrom(LocalDate.now());

                invoiceService.getSupplierInvoice(dto, "supplier")
                              .getContent()
                              .forEach(i -> assertEquals("1234567898", i.getInvoiceNumber()));

            }

            @Test
            @DisplayName("it should return all invoice By dateFrom")
            public void shouldReturnAllInvoiceByDateFrom() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setDateFrom(LocalDate.parse("2021-05-01"));

                invoiceService.getSupplierInvoice(dto, "supplier")
                              .getContent()
                              .forEach(i -> assertEquals(LocalDate.parse("2021-05-01"), i.getInvoiceDate()));

            }

            @Test
            @DisplayName("it should return all invoice Between to date")
            public void shouldReturnAllInvoiceBetweenDateFromAndDateTo() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setDateFrom(LocalDate.parse("2021-05-01"));
                dto.setDateTo(LocalDate.now());

                invoiceService.getSupplierInvoice(dto, "supplier")
                              .getContent()
                              .forEach(i -> assertThat(
                                      i.getInvoiceDate()
                              ).isIn(LocalDate.parse("2021-05-01"), LocalDate.now()));

            }

            @Test
            @DisplayName("it should return all invoice By ageing")
            public void shouldReturnAllInvoiceByAgeing() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setAgeing(ChronoUnit.DAYS.between(LocalDate.parse("2021-05-01"), LocalDate.now()));

                invoiceService.getSupplierInvoice(dto, "supplier")
                              .getContent()
                              .forEach(i -> assertEquals(
                                      LocalDate.parse("2021-05-01"),
                                      i.getInvoiceDate()
                              ));
            }

            @Test
            @DisplayName("it should return all invoice By status")
            public void shouldReturnAllInvoiceByStatus() {
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setStatus(_getStatusList());

                invoiceService.getSupplierInvoice(dto, "supplier")
                              .getContent()
                              .forEach(i -> assertEquals(
                                      InvoiceStatus.IN_REVIEW,
                                      i.getStatus()
                              ));
            }

            @Test
            @DisplayName("it should return all invoice By currencyType")
            public void shouldReturnAllInvoiceByCurrencyType() {
                List<CurrencyType> currencyTypes = new ArrayList<>();
                currencyTypes.add(CurrencyType.GBP);
                InvoiceSearchCriteriaDTO dto = new InvoiceSearchCriteriaDTO();
                dto.setCurrencyType(currencyTypes);

                assertEquals(1, invoiceService.getSupplierInvoice(dto, "supplier")
                                              .getNumberOfElements());
            }
        }
    }
}