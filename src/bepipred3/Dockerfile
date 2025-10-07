FROM nvidia/cuda:12.3.2-runtime-ubuntu22.04

LABEL app="BepiPred"
LABEL version="3.0"
LABEL description="Prediction of potential B-cell epitopes from protein sequence"

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        python3 python3-pip python3-venv libgomp1 unzip curl && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

RUN ln -sf /usr/bin/python3 /usr/bin/python

COPY bepipred3.zip /tmp/bepipred3.zip
RUN unzip /tmp/bepipred3.zip -d / && rm -f /tmp/bepipred3.zip

RUN python3 -m venv /venv
ENV PATH="/venv/bin:$PATH"

RUN printf "fair-esm==1.0.3\n\
importlib-resources==5.8.0\n\
numpy==1.26.4\n\
pandas==1.4.3\n\
plotly==5.8.0\n\
python-dateutil==2.8.2\n\
pytz==2022.1\n\
six==1.16.0\n\
tenacity==8.0.1\n\
torch==2.2.2\n\
typing-extensions>=4.8.0\n\
zipp==3.8.0\n" > /tmp/requirements.txt

RUN pip install --no-cache-dir -r /tmp/requirements.txt && rm -f /tmp/requirements.txt

ENV PATH=/usr/local/cuda/bin:$PATH
ENV LD_LIBRARY_PATH=/usr/local/cuda/lib64:$LD_LIBRARY_PATH

RUN mkdir -p /models && \
    curl -o /models/esm2_t33_650M_UR50D-contact-regression.pt https://dl.fbaipublicfiles.com/fair-esm/regression/esm2_t33_650M_UR50D-contact-regression.pt && \
    curl -o /models/esm2_t33_650M_UR50D.pt https://dl.fbaipublicfiles.com/fair-esm/models/esm2_t33_650M_UR50D.pt

WORKDIR /

RUN apt-get remove --purge -y unzip curl && \
    apt-get autoremove -y && \
    apt-get clean && rm -rf /var/lib/apt/lists/*
    
ENTRYPOINT ["python", "bepipred3_CLI.py"]
