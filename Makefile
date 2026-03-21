.PHONY: bootstrap bootstrap-dirs bootstrap-backend bootstrap-frontend

bootstrap: bootstrap-dirs bootstrap-backend bootstrap-frontend
	@echo 'Bootstrap completed\!'

bootstrap-dirs:
	python3 bootstrap_step1.py
