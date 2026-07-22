import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://127.0.0.1:8080";

export const options = {
  scenarios: {
    catalog: {
      executor: "ramping-vus",
      startVUs: 1,
      stages: [
        { duration: "15s", target: 20 },
        { duration: "30s", target: 20 },
        { duration: "15s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<500", "p(99)<1000"],
  },
};

export default function () {
  const catalog = http.get(`${baseUrl}/api/products?page=0&size=12`);
  check(catalog, { "catalog 200": (response) => response.status === 200 });

  const search = http.get(`${baseUrl}/api/products/search?q=kitob&page=0&size=12`);
  check(search, { "search 200": (response) => response.status === 200 });

  const regions = http.get(`${baseUrl}/api/orders/regions`);
  check(regions, { "regions 200": (response) => response.status === 200 });
  sleep(1);
}
