# Contributing to System Design Learning Journey

Thank you for your interest in contributing! This repository is an open-source, hands-on learning project dedicated to **System Design**, **Distributed Systems Architecture**, and **Backend Engineering in Java**.

We welcome contributions from engineers of all experience levels—from fixing typos to contributing new architecture case studies, interview questions, or Java simulation modules.

---

## 🎯 What You Can Contribute

You are welcome to contribute across several areas:

1. **System Design Theory & Case Studies**:
   - Deep-dives into distributed system components (e.g., Consensus protocols, Consistent Hashing, Distributed Tracing, Circuit Breakers).
   - Real-world architecture breakdowns (similar to our [Twitter/X Case Study](theory/twitter_x_system_design/README.md)).
   - Architecture diagrams (using Mermaid.js or visual diagram exports).

2. **Practical Java Code Simulations**:
   - Self-contained Java demonstrations of distributed systems algorithms (e.g., Consistent Hashing ring, Leaky Bucket rate limiter, Vector Clocks).
   - Improvements, bug fixes, or unit tests for existing modules.

3. **Production Interview Preparation**:
   - Production failure scenarios, triage steps, and architectural trade-offs.
   - Clarifications and additions to the [E-Commerce 100 System Design Questions](theory/interview_prep/ecommerce_100_system_design_questions.md).

4. **Documentation & Quality**:
   - Fixing broken links, typos, ambiguous explanations, or outdated dependencies.
   - Improving explanations for beginners without sacrificing technical depth.

---

## 🚀 Contribution Workflow (Fork & Pull Request)

We follow the standard GitHub Fork-and-Pull-Request workflow:

### 1. Fork and Clone
1. Fork the repository to your own GitHub account by clicking **Fork** at the top right of the [repository page](https://github.com/CyberVerve07/Systems_Designs_learning).
2. Clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/Systems_Designs_learning.git
   cd Systems_Designs_learning
   ```
3. Set the upstream remote to keep your fork synced:
   ```bash
   git remote add upstream https://github.com/CyberVerve07/Systems_Designs_learning.git
   ```

### 2. Create a Topic Branch
Create a descriptive branch for your work:
```bash
git checkout -b feat/consistent-hashing-example
# or: git checkout -b fix/jwt-role-parsing
# or: git checkout -b docs/add-circuit-breaker-guide
```

### 3. Make Your Changes
Implement your changes following the contribution guidelines below.

### 4. Build and Test Locally
Verify that all modified Maven modules compile and test cleanly:
```bash
# Example: navigating to your modified module and compiling
cd code/<topic>/<module>
mvn clean test-compile
```

### 5. Commit and Push
Write clear, concise commit messages following conventional commit style (e.g., `feat:`, `fix:`, `docs:`, `refactor:`):
```bash
git add .
git commit -m "feat(rate-limiter): add leaky bucket algorithm simulation"
git push origin feat/leaky-bucket-algorithm
```

### 6. Open a Pull Request
1. Open a Pull Request from your branch against the `main` branch of `CyberVerve07/Systems_Designs_learning`.
2. Fill out the PR template with a clear explanation of what was changed and how it was verified.
3. Address any review feedback.

---

## 💻 Guidelines for Java Code Contributions

- **Java Version**: Code should be compatible with Java 17 (or Java 11 where configured in `pom.xml`).
- **Self-Contained & Runnable**: Each example should include a standalone entry point (`main` method or demo class) that runs out-of-the-box or clearly specifies external service requirements (like Redis or Kafka).
- **Concurrency & Thread Safety**: System design simulations often model concurrent workloads. Ensure shared data structures are thread-safe (e.g., using `ConcurrentHashMap`, `AtomicLong`, or explicit synchronization).
- **Clean Logging & Feedback**: Use `System.out` or SLF4J (`slf4j-simple`) to produce clear, human-readable terminal output that illustrates the concept step-by-step.
- **Minimal Dependencies**: Keep third-party dependencies to an essential minimum. Favor standard Java APIs unless the topic specifically covers an external tool (like Jedis for Redis, Kafka Client, or JJWT).
- **No Build Artifacts**: Ensure build directories (`target/`, `.class`, `.jar`) are not committed (already excluded in `.gitignore`).

---

## 📖 Guidelines for System Design & Theory Contributions

- **Structure & Clarity**: Organize notes with clear headings, bullet points, and real-world analogies.
- **Focus on Trade-offs**: Good system design is about trade-offs (e.g., latency vs. consistency, throughput vs. complexity, storage vs. compute). Always explain the *why* behind architectural decisions.
- **Visuals & Diagrams**: Mermaid markdown diagrams are highly encouraged for readability directly on GitHub:
  ```mermaid
  graph LR
      Client --> LB[Load Balancer]
      LB --> S1[Server 1]
      LB --> S2[Server 2]
  ```
- **Accuracy**: Avoid unsubstantiated benchmarks or exaggerated performance claims. Ground explanations in standard industry engineering principles.

---

## 🤝 Community Standards & Code of Conduct

- Be respectful, constructive, and welcoming to all contributors.
- Provide thoughtful reviews and accept feedback with an open mindset.
- Help make distributed systems accessible to engineers at all stages of their journey.

---

## ❓ Need Help?

If you have questions before opening a PR or want to discuss a new topic idea, feel free to open a [GitHub Discussion](https://github.com/CyberVerve07/Systems_Designs_learning/discussions) or submit a [Topic Proposal Issue](https://github.com/CyberVerve07/Systems_Designs_learning/issues/new?template=feature_request.md).
