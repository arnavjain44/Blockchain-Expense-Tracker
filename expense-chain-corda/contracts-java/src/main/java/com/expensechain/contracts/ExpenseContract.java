package com.expensechain.contracts;

import com.expensechain.states.ExpenseState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * Validates the core ledger rules for recording an expense.
 */
public class ExpenseContract implements Contract {

    public static final String ID = "com.expensechain.contracts.ExpenseContract";

    public interface Commands extends CommandData {
        class Create implements Commands {}
    }

    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

        if (commandData instanceof Commands.Create) {
            requireThat(require -> {
                require.using("No inputs should be consumed when creating an expense.",
                        tx.getInputStates().isEmpty());
                require.using("Exactly one ExpenseState should be created.",
                        tx.getOutputStates().size() == 1);

                ExpenseState out = (ExpenseState) tx.getOutputStates().get(0);

                require.using("Expense amount must be positive.",
                        out.getAmountMinorUnits() > 0);
                require.using("Expense must have at least one participant.",
                        !out.getParticipants().isEmpty());
                require.using("Payer must be one of the participants.",
                        out.getParticipants().stream().anyMatch(p -> p.equals(out.getPayer())));
                require.using("expenseId must not be blank.",
                        out.getExpenseId() != null && !out.getExpenseId().trim().isEmpty());
                require.using("groupId must not be blank.",
                        out.getGroupId() != null && !out.getGroupId().trim().isEmpty());
                require.using("Split type must be EQUAL or CUSTOM.",
                        "EQUAL".equals(out.getSplitType()) || "CUSTOM".equals(out.getSplitType()));

                Map<String, Long> splits = out.getSplitDetails();
                require.using("splitDetails must cover every participant exactly once.",
                        splits != null && splits.size() == out.getParticipants().size());

                long splitSum = splits.values().stream().mapToLong(Long::longValue).sum();
                require.using("Sum of splitDetails must equal the total expense amount.",
                        splitSum == out.getAmountMinorUnits());

                List<PublicKey> requiredSigners = Collections.singletonList(out.getPayer().getOwningKey());
                require.using("Payer must sign the expense transaction.",
                        command.getSigners().containsAll(requiredSigners));

                return null;
            });
        } else {
            throw new IllegalArgumentException("Unrecognised command: " + commandData);
        }
    }
}
