package com.hcl.capstoneserver.invoice;

import com.hcl.capstoneserver.invoice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@CrossOrigin
@RestController
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/api/invoices/create")
    public ResponseEntity<ClientViewInvoiceDTO> createInvoice(@RequestBody CreateInvoiceDTO dto, Principal principal) {
        return new ResponseEntity<>(invoiceService.createInvoice(dto, principal.getName()), HttpStatus.CREATED);
    }

    @PutMapping("/api/invoices/update")
    public ResponseEntity<ClientViewInvoiceDTO> updateInvoice(@RequestBody UpdateInvoiceDTO dto, Principal principal) {
        return new ResponseEntity<>(invoiceService.updateInvoice(dto, principal.getName()), HttpStatus.CREATED);
    }

    @PutMapping("/api/invoices/update/status")
    public ResponseEntity<BankViewInvoiceDTO> setStatus(@RequestBody StatusUpdateInvoiceDTO dto, Principal principal) {
        return new ResponseEntity<>(invoiceService.statusUpdate(dto, principal.getName()), HttpStatus.CREATED);
    }

    @DeleteMapping("/api/invoices/delete/{id}")
    public ResponseEntity<Long> deleteInvoice(@PathVariable Integer id, Principal principal) {
        return new ResponseEntity<>(invoiceService.deleteInvoice(id, principal.getName()), HttpStatus.OK);
    }

    @GetMapping("/api/invoices/retrieve/bank")
    public Page<BankViewInvoiceDTO> getAllInvoice(@RequestBody InvoiceSearchCriteriaDTO dto, Principal principal) {
        return invoiceService.getBankInvoice(dto, principal.getName());
    }

    @GetMapping("/api/invoices/retrieve/client")
    public Page<ClientViewInvoiceDTO> getClientAllInvoice(
            @RequestBody InvoiceSearchCriteriaDTO dto,
            Principal principal
    ) {
        return invoiceService.getClientInvoice(dto, principal.getName());
    }

    @GetMapping("/api/invoices/retrieve/supplier")
    public Page<SupplierVIewInvoiceDTO> getSupplierAllInvoice(
            @RequestBody InvoiceSearchCriteriaDTO dto,
            Principal principal
    ) {
        return invoiceService.getSupplierInvoice(dto, principal.getName());
    }
}
