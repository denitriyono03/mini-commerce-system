# ==========================================
#  Mini E-Commerce: Catalog Service Makefile
# ==========================================

.PHONY: help clean build test run debug

# Mendefinisikan Maven Wrapper agar command konsisten di semua mesin
MVN = ./mvnw

help: ## Menampilkan daftar perintah yang tersedia
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

clean: ## Menghapus folder target/ (hasil build sebelumnya)
	$(MVN) clean

build: ## Melakukan kompilasi dan membungkus project menjadi file JAR (tanpa menjalankan unit test)
	$(MVN) clean package -DskipTests

test: ## Menjalankan semua unit test dan integration test
	$(MVN) test

run: ## Menjalankan aplikasi Spring Boot secara normal
	$(MVN) spring-boot:run

debug: ## Menjalankan aplikasi dalam mode Debug (Membuka port 5005 untuk VS Code)
	$(MVN) spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"