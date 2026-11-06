package parking.model;

import parking.pricing.Tariffs;

/**
 * NEW VEHICLE CATEGORY 2 of 2.
 *
 * This is the class that justifies the whole redesign.  Its tariff depends on
 * the TIME OF DAY, which is not a property of the van, so under the original
 * design there was nothing sensible for an overridden ratePerHour() to return.
 * Here the van simply names a different rule, and the rule knows about clocks.
 */
public final class DeliveryVan extends Vehicle {
    public DeliveryVan(String regNo) {
        super(regNo, VehicleCategory.DELIVERY_VAN, Tariffs.deliveryVan());
    }
}
