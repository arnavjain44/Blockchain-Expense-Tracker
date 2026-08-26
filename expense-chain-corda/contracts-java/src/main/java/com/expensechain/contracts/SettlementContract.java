package com.expensechain.contracts;

import com.expensechain.states.SettlementState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * Validates the core ledger rules for recording a debt settlement.
 */
public class SettlementContract implements Contract {

    public static final String ID = "com.expensechain.contracts.SettlementContract";

    public interface Commands extends CommandData {
        class Create implements Commands {}
    }

    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

        if (commandData instanceof Commands.Create) {
            requireThat(require -> {
                require.using("No inputs should be consumed when recording a settlement.",
                        tx.getInputStates().isEmpty());
                require.using("Exactly one SettlementState should be created.",
                        tx.getOutputStates().size() == 1);

                SettlementState out = (SettlementState) tx.getOutputStates().get(0);

                require.using("Settlement amount must be positive.",
                        out.getAmountMinorUnits() > 0);
                require.using("Payer and payee must be different parties.",
                        !out.getPayer().equals(out.getPayee()));
                require.using("settlementId must not be blank.",
                        out.getSettlementId() != null && !out.getSettlementId().trim().isEmpty());
                require.using("groupId must not be blank.",
                        out.getGroupId() != null && !out.getGroupId().trim().isEmpty());
                require.using("currency must not be blank.",
                        out.getCurrency() != null && !out.getCurrency().trim().isEmpty());

                List<PublicKey> requiredSigners = Collections.singletonList(out.getPayer().getOwningKey());
                require.using("Payer must sign the settlement transaction.",
                        command.getSigners().containsAll(requiredSigners));

                return null;
            });
        } else {
            throw new IllegalArgumentException("Unrecognised command: " + commandData);
        }
    }
}
