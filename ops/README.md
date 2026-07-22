# Backend performance

## Monitoring

Actuator asosiy HTTP portdan ajratilgan `8081` management portda ishlaydi:

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/prometheus`

Prometheus va Grafana yagona production Compose ichiga kiritilgan. Ishga tushirish:

```bash
docker compose -f docker-compose.prod.yaml up -d
```

Portlar faqat localhost'ga ochiladi: Prometheus default `9090`, Grafana default
`3001`. Ularni `.env` ichidagi `PROMETHEUS_PORT` va `GRAFANA_PORT` bilan almashtirish
mumkin. `GRAFANA_ADMIN_PASSWORD` majburiy.

## Load test

Backend ishga tushgach katalog smoke testi:

```bash
k6 run load-tests/catalog-smoke.js
```

Boshqa server uchun:

```bash
k6 run -e BASE_URL=https://api.example.uz load-tests/catalog-smoke.js
```

Test p95 < 500 ms, p99 < 1 s va xatolar < 1% thresholdlarini tekshiradi.

## PostgreSQL

Production serverda `pg_stat_statements`ni server konfiguratsiyasida yoqib, sekin
querylarni `total_exec_time`, `mean_exec_time` va `calls` bo'yicha kuzating. Indekslar
`0015-performance-indexes.sql` migratsiyasida boshqariladi.
