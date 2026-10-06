package com.systemdesign.cqrs;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * CQRS PATTERN — Complete Implementation
 * ═══════════════════════════════════════════════════════════════
 *
 * Architecture:
 *   Commands (writes) → Write Store (simulated MySQL)
 *                    → Domain Events published
 *                    → Event Handler updates Read Store
 *
 *   Queries (reads)  → Read Store (simulated Redis/Elasticsearch)
 *
 * Components:
 *   - Commands: PlaceOrderCommand, UpdateOrderStatusCommand
 *   - Events: OrderPlacedEvent, OrderStatusUpdatedEvent
 *   - Write Side: OrderWriteRepository (normalized)
 *   - Read Side: OrderReadRepository (denormalized)
 *   - Command Handlers: PlaceOrderCommandHandler
 *   - Query Handlers: GetOrderQueryHandler
 *   - Event Bus: InMemoryEventBus
 *   - Event Handler: OrderEventHandler (updates read model)
 */
public class CQRSDemo {

    // ─── Value Objects & Models ─────────────────────────────────

    /** Immutable command — represents intent to change state */
    record PlaceOrderCommand(String orderId, String customerId, List<OrderItem> items) {}

    record UpdateOrderStatusCommand(String orderId, String newStatus) {}

    record OrderItem(String productId, String productName, int quantity, double price) {}

    /** Events — past tense, immutable facts */
    record OrderPlacedEvent(String eventId, String orderId, String customerId,
                            List<OrderItem> items, double total, Instant timestamp) {}

    record OrderStatusUpdatedEvent(String eventId, String orderId,
                                   String oldStatus, String newStatus, Instant timestamp) {}

    // ─── Write Model (Normalized, like MySQL rows) ──────────────

    static class OrderWriteModel {
        String orderId;
        String customerId;
        String status;
        double total;
        List<OrderItem> items;
        Instant createdAt;

        OrderWriteModel(String orderId, String customerId, List<OrderItem> items) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.items = new ArrayList<>(items);
            this.total = items.stream().mapToDouble(i -> i.price() * i.quantity()).sum();
            this.status = "PENDING";
            this.createdAt = Instant.now();
        }
    }

    // ─── Read Model (Denormalized, pre-joined, like Elasticsearch doc) ───

    static class OrderReadModel {
        String orderId;
        String customerName;    // Joined from customer service
        String customerEmail;   // Joined from customer service
        String status;
        double total;
        int itemCount;
        List<String> productNames;  // Flattened for quick display
        Instant createdAt;
        Instant lastUpdated;

        @Override
        public String toString() {
            return String.format("OrderReadModel{id=%s, customer=%s, status=%s, " +
                            "total=%.2f, items=%s}",
                    orderId, customerName, status, total, productNames);
        }
    }

    // ─── Event Bus ──────────────────────────────────────────────

    interface EventHandler<E> {
        void handle(E event);
    }

    static class InMemoryEventBus {
        private final Map<Class<?>, List<EventHandler<?>>> handlers = new HashMap<>();

        @SuppressWarnings("unchecked")
        public <E> void publish(E event) {
            List<EventHandler<?>> eventHandlers = handlers.getOrDefault(event.getClass(), List.of());
            System.out.println("[EventBus] 📢 Publishing " + event.getClass().getSimpleName());
            eventHandlers.forEach(h -> ((EventHandler<E>) h).handle(event));
        }

        public <E> void subscribe(Class<E> eventClass, EventHandler<E> handler) {
            handlers.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(handler);
        }
    }

    // ─── Repositories ───────────────────────────────────────────

    static class OrderWriteRepository {
        private final Map<String, OrderWriteModel> store = new ConcurrentHashMap<>();

        public void save(OrderWriteModel order) {
            store.put(order.orderId, order);
            System.out.println("[WriteDB] 💾 Saved order: " + order.orderId + " | status=" + order.status);
        }

        public Optional<OrderWriteModel> findById(String orderId) {
            return Optional.ofNullable(store.get(orderId));
        }
    }

    static class OrderReadRepository {
        private final Map<String, OrderReadModel> store = new ConcurrentHashMap<>();

        public void save(OrderReadModel model) {
            store.put(model.orderId, model);
            System.out.println("[ReadDB] 📖 Indexed read model: " + model.orderId);
        }

        public Optional<OrderReadModel> findById(String orderId) {
            return Optional.ofNullable(store.get(orderId));
        }

        public List<OrderReadModel> findByStatus(String status) {
            return store.values().stream()
                    .filter(m -> m.status.equals(status))
                    .collect(Collectors.toList());
        }

        public List<OrderReadModel> findAll() {
            return new ArrayList<>(store.values());
        }
    }

    // ─── Event Handler (bridges write → read side) ──────────────

    static class OrderEventHandler {
        private final OrderReadRepository readRepo;
        // Simulated customer lookup (would be a real HTTP call in production)
        private final Map<String, String[]> customers = Map.of(
                "cust-1", new String[]{"Alice", "alice@example.com"},
                "cust-2", new String[]{"Bob", "bob@example.com"}
        );

        OrderEventHandler(OrderReadRepository readRepo) {
            this.readRepo = readRepo;
        }

        void onOrderPlaced(OrderPlacedEvent event) {
            System.out.println("[EventHandler] 🔄 Updating read model for OrderPlaced: " + event.orderId());

            OrderReadModel model = new OrderReadModel();
            model.orderId = event.orderId();
            model.status = "PENDING";
            model.total = event.total();
            model.itemCount = event.items().size();
            model.productNames = event.items().stream()
                    .map(OrderItem::productName)
                    .collect(Collectors.toList());
            model.createdAt = event.timestamp();
            model.lastUpdated = event.timestamp();

            // Simulate lookup of customer details
            String[] customer = customers.getOrDefault(event.customerId(),
                    new String[]{"Unknown", "unknown@example.com"});
            model.customerName = customer[0];
            model.customerEmail = customer[1];

            readRepo.save(model);
        }

        void onOrderStatusUpdated(OrderStatusUpdatedEvent event) {
            System.out.println("[EventHandler] 🔄 Updating read model for StatusUpdate: " + event.orderId());
            readRepo.findById(event.orderId()).ifPresent(model -> {
                model.status = event.newStatus();
                model.lastUpdated = event.timestamp();
                readRepo.save(model);
            });
        }
    }

    // ─── Command Handlers ───────────────────────────────────────

    static class OrderCommandHandler {
        private final OrderWriteRepository writeRepo;
        private final InMemoryEventBus eventBus;

        OrderCommandHandler(OrderWriteRepository writeRepo, InMemoryEventBus eventBus) {
            this.writeRepo = writeRepo;
            this.eventBus = eventBus;
        }

        public void handle(PlaceOrderCommand cmd) {
            System.out.println("\n[CommandHandler] 📝 Processing PlaceOrderCommand: " + cmd.orderId());

            // 1. Business validation
            if (cmd.items() == null || cmd.items().isEmpty()) {
                throw new IllegalArgumentException("Order must have at least one item");
            }

            // 2. Create domain object
            OrderWriteModel order = new OrderWriteModel(cmd.orderId(), cmd.customerId(), cmd.items());

            // 3. Save to write store
            writeRepo.save(order);

            // 4. Publish domain event (this updates the read model via event handler)
            eventBus.publish(new OrderPlacedEvent(
                    UUID.randomUUID().toString(),
                    order.orderId,
                    order.customerId,
                    order.items,
                    order.total,
                    order.createdAt
            ));
        }

        public void handle(UpdateOrderStatusCommand cmd) {
            System.out.println("\n[CommandHandler] 📝 Processing UpdateOrderStatusCommand: " + cmd.orderId());

            OrderWriteModel order = writeRepo.findById(cmd.orderId())
                    .orElseThrow(() -> new NoSuchElementException("Order not found: " + cmd.orderId()));

            String oldStatus = order.status;
            order.status = cmd.newStatus();
            writeRepo.save(order);

            eventBus.publish(new OrderStatusUpdatedEvent(
                    UUID.randomUUID().toString(),
                    order.orderId,
                    oldStatus,
                    cmd.newStatus(),
                    Instant.now()
            ));
        }
    }

    // ─── Query Handlers ─────────────────────────────────────────

    static class OrderQueryHandler {
        private final OrderReadRepository readRepo;

        OrderQueryHandler(OrderReadRepository readRepo) {
            this.readRepo = readRepo;
        }

        public Optional<OrderReadModel> getOrder(String orderId) {
            System.out.println("[QueryHandler] 🔍 GET /orders/" + orderId + " (from Read Model)");
            return readRepo.findById(orderId);
        }

        public List<OrderReadModel> getOrdersByStatus(String status) {
            System.out.println("[QueryHandler] 🔍 GET /orders?status=" + status + " (from Read Model)");
            return readRepo.findByStatus(status);
        }

        public List<OrderReadModel> getAllOrders() {
            System.out.println("[QueryHandler] 🔍 GET /orders (from Read Model)");
            return readRepo.findAll();
        }
    }

    // ─── MAIN DEMO ──────────────────────────────────────────────

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║               CQRS PATTERN DEMO                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        // Wire up the components
        InMemoryEventBus eventBus = new InMemoryEventBus();
        OrderWriteRepository writeRepo = new OrderWriteRepository();
        OrderReadRepository readRepo = new OrderReadRepository();
        OrderEventHandler eventHandler = new OrderEventHandler(readRepo);

        // Subscribe event handlers
        eventBus.subscribe(OrderPlacedEvent.class, eventHandler::onOrderPlaced);
        eventBus.subscribe(OrderStatusUpdatedEvent.class, eventHandler::onOrderStatusUpdated);

        OrderCommandHandler commandHandler = new OrderCommandHandler(writeRepo, eventBus);
        OrderQueryHandler queryHandler = new OrderQueryHandler(readRepo);

        // ─── COMMAND SIDE (Writes) ─────────────────────────────
        System.out.println("\n════ COMMAND SIDE (Writes) ════");

        // Place Order 1
        commandHandler.handle(new PlaceOrderCommand("order-001", "cust-1",
                List.of(
                        new OrderItem("prod-iPhone", "iPhone 15", 1, 999.99),
                        new OrderItem("prod-case", "iPhone Case", 2, 19.99)
                )
        ));

        // Place Order 2
        commandHandler.handle(new PlaceOrderCommand("order-002", "cust-2",
                List.of(new OrderItem("prod-mac", "MacBook Pro", 1, 2499.99))
        ));

        // Update status
        commandHandler.handle(new UpdateOrderStatusCommand("order-001", "CONFIRMED"));
        commandHandler.handle(new UpdateOrderStatusCommand("order-002", "SHIPPED"));

        // ─── QUERY SIDE (Reads) ────────────────────────────────
        System.out.println("\n════ QUERY SIDE (Reads from Denormalized Read Model) ════");

        // Query single order — fast lookup, no joins needed!
        queryHandler.getOrder("order-001").ifPresent(o ->
                System.out.println("Found: " + o));

        // Query by status
        System.out.println("\nOrders with SHIPPED status:");
        queryHandler.getOrdersByStatus("SHIPPED")
                .forEach(o -> System.out.println("  - " + o));

        // Query all
        System.out.println("\nAll orders:");
        queryHandler.getAllOrders()
                .forEach(o -> System.out.println("  - " + o));

        System.out.println("\n✅ CQRS Demo complete!");
        System.out.println("Notice: Reads from denormalized model — no JOINs required!");
    }
}
