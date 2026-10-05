package com.fap.common.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Defers side effects (cache invalidation, mail) until the current transaction commits, so they
 * never act on changes that are later rolled back or not yet visible to other connections.
 */
public final class AfterCommit {

	private AfterCommit() {
	}

	/** Runs {@code action} after commit, or immediately when no transaction is active. */
	public static void run(Runnable action) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			action.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				action.run();
			}
		});
	}
}
