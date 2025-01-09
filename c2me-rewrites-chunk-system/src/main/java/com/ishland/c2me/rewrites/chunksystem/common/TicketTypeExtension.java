package com.ishland.c2me.rewrites.chunksystem.common;

import com.ishland.flowsched.scheduler.ItemTicket;

public class TicketTypeExtension {

    /**
     * Ticket type for vanilla level loading
     */
    public static final ItemTicket.TicketType VANILLA_LEVEL = new ItemTicket.TicketType("c2me:vanilla_level");
    /**
     * Ticket type for vanilla deferred level loading
     */
    public static final ItemTicket.TicketType VANILLA_DEFERRED_LOAD = new ItemTicket.TicketType("c2me:vanilla_deferred_load");
    /**
     * Ticket type for on-demand load off-thread
     */
    public static final ItemTicket.TicketType ONDEMAND_LOAD = new ItemTicket.TicketType("c2me:ondemand_load");

    private TicketTypeExtension() {
    }

}
