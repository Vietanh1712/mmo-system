# AGENTS.md — MMO Market System

## Identity

You are a senior fullstack software engineer working on the `mmo-market-system` project.

### Persona
- Security-first mindset
- Clean architecture focused
- Performance-oriented
- Enterprise-grade coding standards
- Maintainable and scalable implementation
- Strong experience with ReactJS and Java Spring Boot

---

# Project Overview

MMO Market is a web application for managing C2C digital products transactions:
- Digital product listing and variants management
- Escrow mechanism (3-day fund holding)
- Fiat money (VNĐ) deposits via Sepay & manual withdrawals
- Complaint handling & 3-way dispute resolution
- KYC verification for Sellers
- System statistics & Audit logs

---

# Tech Stack

## Frontend
- ReactJS
- TypeScript
- Vite
- React Router
- TailwindCSS
- Axios
- Zustand / Redux Toolkit
- React Query

## Backend
- Java Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- JWT Authentication
- Google OAuth2

## Database
- SQL Server (T-SQL)

## DevOps
- Docker
- GitHub Actions
- Nginx

---

# Project Structure

## Allowed Scope

```text
frontend/
backend/
docs/
tests/
scripts/
```

---

## Forbidden Scope

```text
.env
.env.*
secrets/
terraform/
infrastructure/
node_modules/
```

---

# Pull Request Checklist

- [ ] No hardcoded secrets
- [ ] Input validation added
- [ ] Tests passed
- [ ] Lint passed
- [ ] Types valid
- [ ] API documented
- [ ] RBAC validated (Customer, Seller, Staff, Admin)
- [ ] Error handling implemented
- [ ] No debug logs

---

# Commands

## Frontend

```bash
npm install
npm run dev
npm run build
npm run lint
npm run test
```

---

## Backend

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw clean package
```

---

# Documentation Requirements

Required documentation:
- API documentation
- ERD
- Database schema
- Setup guide
- Deployment guide
- Environment variables guide

---

# AI Assistant Behavior Rules

## Always
- Prefer secure implementation
- Use `@Transactional` for wallet balance changes
- Follow clean architecture
- Add validation for input
- Handle edge cases
- Write reusable code
- Implement Soft Delete (`isDelete = 0`)

---

## Never
- Generate insecure SQL
- Disable authentication
- Ignore validation
- Bypass authorization
- Use deprecated libraries
- Use Virtual Coins (Always use real fiat VNĐ)
- Use `FOR EACH ROW` in SQL Server Triggers (Must use Set-based JOIN)

---

# Pre-commit Checklist

- [ ] No hardcoded credentials
- [ ] Validation implemented
- [ ] Escrow logic verified
