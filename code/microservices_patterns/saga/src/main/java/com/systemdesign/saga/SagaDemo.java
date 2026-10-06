package com.systemdesign.saga;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ═══════════════════════════════════════════════════════════════
 * SAGA PATTERN — Orchestration-Based Implementation
 * ═══════════════════════════════════════════════════════════════
 *
 * Flow: Place Order Saga
 *   Step 1: Create Order (Order Service)
 *   Step 2: Process Payment (Payment Service)
 *   Step 3: Reserve Inventory (Inventory Service)
 *   Step 4: Confirm Order (Order Service)
 *
 * On failure at any step:
 *   Compensating transactions execute in reverse order
 *
 * This implementation uses an Orchestrator pattern:
 *   - SagaOrchestrator controls the entire flow
 *   - Each service is called explicitly by the orchestrator
 *   - Saga state is persisted in the saga store
 */
public class SagaDemo {

    // ─── Saga Status ────────────────────────────────────────────

    enum SagaStatus { STARTED, STEP_COMPLETED, COMPENSATING, COMPLETED, FAILED }

    // ─── Domain Models ──────────────────────────────────────────

    record OrderRequest(String orderId, String customerId, String productId,
                        int quantity, double amount) {}

    // ─── Saga State (persisted for durability) ──────────────────

    static class SagaState {
        final String sagaId;
        final OrderRequest request;
        SagaStatus status;
        int currentStep;
        final List<String> completedSteps = new ArrayList<>();
        final List<String> log = new ArrayList<>();

        SagaState(String sagaId, OrderRequest request) {
            this.sagaId = sagaId;
            this.request = request;
            this.status = SagaStatus.STARTED;
            this.currentStep = 0;
        }

        void addLog(String msg) {
            log.add("[" + new Date() + "] " + msg);
            System.out.println("[Saga:" + sagaId + "] " + msg);
        }
    }

    // ─── Simulated Microservices ─────────────────────────────────

    static class OrderService {
        private final Map<String, String> orders = new ConcurrentHashMap<>();

        public void createOrder(String orderId, String customerId) {
            orders.put(orderId, "PENDING");
            System.out.println("[OrderService]   ✅ Created order " + orderId + " (status=PENDING)");
        }

        public void confirmOrder(String orderId) {
            orders.put(orderId, "CONFIRMED");
            System.out.println("[OrderService]   ✅ Confirmed order " + orderId);
        }

        public void cancelOrder(String orderId) {
            orders.put(orderId, "CANCELLED");
            System.out.println("[OrderService]   🔙 COMPENSATE: Cancelled order " + orderId);
        }
    }

    static class PaymentService {
        private final Map<String, Double> payments = new ConcurrentHashMap<>();
        private boolean shouldFail = false;

        public void processPayment(String orderId, double amount) {
            if (shouldFail) {
                throw new RuntimeException("Payment declined: insufficient funds");
            }
            payments.put(orderId, amount);
            System.out.printf("[PaymentService] ✅ Payment processed for order %s: $%.2f%n",
                    orderId, amount);
        }

        public void refundPayment(String orderId) {
            Double amount = payments.remove(orderId);
            if (amount != null) {
                System.out.printf("[PaymentService] 🔙 COMPENSATE: Refunded $%.2f for order %s%n",
                        amount, orderId);
            }
        }

        public void setFailure(boolean fail) { this.shouldFail = fail; }
    }

    static class InventoryService {
        private final Map<String, Integer> reservations = new ConcurrentHashMap<>();
        private boolean shouldFail = false;

        public void reserveInventory(String productId, int quantity, String orderId) {
            if (shouldFail) {
                throw new RuntimeException("Inventory unavailable: only 0 units left for " + productId);
            }
            reservations.put(orderId, quantity);
            System.out.printf("[InventoryService] ✅ Reserved %d units of %s for order %s%n",
                    quantity, productId, orderId);
        }

        public void releaseInventory(String productId, int quantity, String orderId) {
            reservations.remove(orderId);
            System.out.printf("[InventoryService] 🔙 COMPENSATE: Released %d units of %s for order %s%n",
                    quantity, productId, orderId);
        }

        public void setFailure(boolean fail) { this.shouldFail = fail; }
    }

    // ─── Saga Orchestrator ───────────────────────────────────────

    static class OrderSagaOrchestrator {
        private final OrderService orderService;
        private final PaymentService paymentService;
        private final InventoryService inventoryService;
        private final Map<String, SagaState> sagaStore = new ConcurrentHashMap<>();

        OrderSagaOrchestrator(OrderService os, PaymentService ps, InventoryService is) {
            this.orderService = os;
            this.paymentService = ps;
            this.inventoryService = is;
        }

        /**
         * Executes the Place Order Saga with full compensation on failure.
         */
        public SagaState execute(OrderRequest request) {
            String sagaId = "saga-" + UUID.randomUUID().toString().substring(0, 8);
            SagaState state = new SagaState(sagaId, request);
            sagaStore.put(sagaId, state);

            state.addLog("🚀 Saga STARTED for order: " + request.orderId());

            try {
                // ── Step 1: Create Order ──────────────────────────
                state.addLog("Step 1: Creating order...");
                orderService.createOrder(request.orderId(), request.customerId());
                state.completedSteps.add("CREATE_ORDER");
                state.currentStep = 1;

                // ── Step 2: Process Payment ───────────────────────
                state.addLog("Step 2: Processing payment...");
                paymentService.processPayment(request.orderId(), request.amount());
                state.completedSteps.add("PROCESS_PAYMENT");
                state.currentStep = 2;

                // ── Step 3: Reserve Inventory ─────────────────────
                state.addLog("Step 3: Reserving inventory...");
                inventoryService.reserveInventory(
                        request.productId(), request.quantity(), request.orderId());
                state.completedSteps.add("RESERVE_INVENTORY");
                state.currentStep = 3;

                // ── Step 4: Confirm Order ─────────────────────────
                state.addLog("Step 4: Confirming order...");
                orderService.confirmOrder(request.orderId());
                state.completedSteps.add("CONFIRM_ORDER");

                state.status = SagaStatus.COMPLETED;
                state.addLog("🎉 Saga COMPLETED successfully!");

            } catch (Exception e) {
                state.addLog("❌ Step failed: " + e.getMessage());
                state.status = SagaStatus.COMPENSATING;
                compensate(state);
            }

            return state;
        }

        /**
         * Executes compensating transactions in REVERSE order for completed steps.
         */
        private void compensate(SagaState state) {
            state.addLog("⏪ Starting COMPENSATION for completed steps: " + state.completedSteps);

            // Reverse iteration — undo in reverse order
            List<String> completed = new ArrayList<>(state.completedSteps);
            Collections.reverse(completed);

            for (String step : completed) {
                try {
                    switch (step) {
                        case "RESERVE_INVENTORY":
                            inventoryService.releaseInventory(
                                    state.request.productId(),
                                    state.request.quantity(),
                                    state.request.orderId());
                            break;
                        case "PROCESS_PAYMENT":
                            paymentService.refundPayment(state.request.orderId());
                            break;
                        case "CREATE_ORDER":
                            orderService.cancelOrder(state.request.orderId());
                            break;
                        default:
                            state.addLog("Unknown step to compensate: " + step);
                    }
                } catch (Exception e) {
                    // Log compensation failure — needs manual intervention
                    state.addLog("⚠️ COMPENSATION FAILED for step " + step + ": " + e.getMessage());
                    state.addLog("🚨 MANUAL INTERVENTION REQUIRED!");
                }
            }

            state.status = SagaStatus.FAILED;
            state.addLog("❌ Saga FAILED — all compensations applied.");
        }
    }

    // ─── MAIN DEMO ───────────────────────────────────────────────

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║               SAGA PATTERN DEMO (Orchestration)              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        OrderService orderSvc = new OrderService();
        PaymentService paymentSvc = new PaymentService();
        InventoryService inventorySvc = new InventoryService();
        OrderSagaOrchestrator orchestrator = new OrderSagaOrchestrator(
                orderSvc, paymentSvc, inventorySvc);

        // ──────────────────────────────────────────────────────────
        // Scenario 1: Happy Path — all steps succeed
        // ──────────────────────────────────────────────────────────
        System.out.println("\n════ SCENARIO 1: Happy Path ════");
        SagaState result1 = orchestrator.execute(
                new OrderRequest("order-001", "cust-alice", "prod-iphone", 1, 999.99));
        System.out.println("Final status: " + result1.status);

        // ──────────────────────────────────────────────────────────
        // Scenario 2: Payment fails → compensate Create Order
        // ──────────────────────────────────────────────────────────
        System.out.println("\n════ SCENARIO 2: Payment Failure → Compensate ════");
        paymentSvc.setFailure(true);
        SagaState result2 = orchestrator.execute(
                new OrderRequest("order-002", "cust-bob", "prod-mac", 1, 2499.99));
        System.out.println("Final status: " + result2.status);
        paymentSvc.setFailure(false);

        // ──────────────────────────────────────────────────────────
        // Scenario 3: Inventory fails → compensate Payment + Create Order
        // ──────────────────────────────────────────────────────────
        System.out.println("\n════ SCENARIO 3: Inventory Failure → Multi-step Compensate ════");
        inventorySvc.setFailure(true);
        SagaState result3 = orchestrator.execute(
                new OrderRequest("order-003", "cust-charlie", "prod-sold-out", 5, 50.00));
        System.out.println("Final status: " + result3.status);
        inventorySvc.setFailure(false);

        System.out.println("\n✅ Saga Demo complete!");
    }
}
