package com.bankatm;

import com.bankatm.repository.AccountRepository;
import com.bankatm.repository.InMemoryAccountRepository;
import com.bankatm.service.BankService;
import com.bankatm.ui.ConsoleATM;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        AccountRepository accountRepository = new InMemoryAccountRepository();
        BankService bankService = new BankService(accountRepository);

        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleATM atm = new ConsoleATM(bankService, scanner);
            atm.run();
        }
    }
}
