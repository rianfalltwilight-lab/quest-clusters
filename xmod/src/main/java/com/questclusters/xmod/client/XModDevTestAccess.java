package com.questclusters.xmod.client;

/** Entry point for the dev self-test steps. */
public final class XModDevTestAccess {
    private XModDevTestAccess() {
    }

    public static void register() {
        XModDevTest.register();
    }
}
