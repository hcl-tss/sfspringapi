package com.hcl.capstoneserver.user;

import com.hcl.capstoneserver.user.dto.*;
import com.hcl.capstoneserver.user.entities.AppUser;
import com.hcl.capstoneserver.user.entities.Client;
import com.hcl.capstoneserver.user.entities.Supplier;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.validation.Valid;
import java.security.Principal;

@CrossOrigin
@RestController()
public class UserController {
    private final UserService userService;
    private final ModelMapper mapper;

    public UserController(UserService userService, ModelMapper mapper) {
        this.userService = userService;
        this.mapper = mapper;
    }

    @PostMapping("/api/sign-in")
    public ResponseEntity<JwtWithTypeDTO> signIn(@Valid @RequestBody AppUserWithPasswordDTO dto) {
    	System.out.println("Request received");
        return new ResponseEntity<>(
                userService.signIn(
                        mapper.map(dto, AppUser.class)
                ),
                HttpStatus.OK
        );
    }

    @PostMapping("/api/refresh-token")
    public ResponseEntity<JwtWithTypeDTO> refreshToken(Principal principal) {
        // throw unauthorized error if no user is defined
        if (principal == null)
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "unauthorized");

        return new ResponseEntity<>(
                userService.refreshToken(principal.getName()),
                HttpStatus.OK
        );
    }

    @PostMapping("/api/sign-up/supplier")
    public ResponseEntity<SupplierDTO> signUpSupplier(@Valid @RequestBody PersonWithPasswordDTO dto) {
        return new ResponseEntity<>(
                userService.signUpSupplier(mapper.map(dto, Supplier.class)),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/api/sign-up/client")
    public ResponseEntity<ClientDTO> signUpClient(@Valid @RequestBody PersonWithPasswordDTO dto) {
        return new ResponseEntity<>(
                userService.signUpClient(mapper.map(dto, Client.class)),
                HttpStatus.CREATED
        );
    }

    //check if supplier id exists
    @GetMapping("/api/users/checkSupplierId")
    public ResponseEntity<CheckExistsDTO> checkSupplierId(@RequestParam(name = "supplierId") String supplierId) {
        CheckExistsDTO existsDTO = userService.checkSupplierId(supplierId);
        return new ResponseEntity<>(
                existsDTO,
                existsDTO.isValid() ? HttpStatus.OK : HttpStatus.NOT_FOUND
        );
    }

    // get the client id of the current user
    @GetMapping("/api/users/myClientId")
    public ResponseEntity<String> getMyClientId(Principal principal) {
        String clientId = userService.getClientId(principal.getName());

        if (clientId != null)
            return new ResponseEntity<>(
                    clientId,
                    HttpStatus.OK
            );
        else
            return new ResponseEntity<>(
                    "Client Id Not Found",
                    HttpStatus.NOT_FOUND
            );
    }
}
