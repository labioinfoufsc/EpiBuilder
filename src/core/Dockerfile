FROM bioinfoufsc/bepipred3:latest

LABEL app="EpiBuilder - CLI"
LABEL version="2.0"
LABEL description="Bioinformatics tool environment for epitope prediction"

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=America/Sao_Paulo

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
    tzdata \ 
    maven curl wget && \
    ln -fs /usr/share/zoneinfo/$TZ /etc/localtime && \
    dpkg-reconfigure --frontend noninteractive tzdata && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

RUN pip install biopython

RUN wget -P /tmp https://ftp.ncbi.nlm.nih.gov/blast/executables/blast+/LATEST/ncbi-blast-2.17.0+-x64-linux.tar.gz && \
    tar -xzf /tmp/ncbi-blast-2.17.0+-x64-linux.tar.gz -C /tmp && \
    mv /tmp/ncbi-blast-2.17.0+/bin/blastp /usr/local/bin && \
    mv /tmp/ncbi-blast-2.17.0+/bin/makeblastdb /usr/local/bin && \
    rm -rf /tmp/ncbi-blast-2.17.0+ /tmp/ncbi-blast-2.17.0+-x64-linux.tar.gz

RUN wget -P /tmp https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz && \
    mkdir -p /usr/local/java && \
    tar -xzf /tmp/jdk-21_linux-x64_bin.tar.gz -C /usr/local/java && \
    rm /tmp/jdk-21_linux-x64_bin.tar.gz && \
    ln -sf /usr/local/java/jdk-21* /usr/local/java/default-java
    
ENV JAVA_HOME=/usr/local/java/default-java
ENV PATH=$JAVA_HOME/bin:$PATH

RUN curl -s https://get.nextflow.io | bash && \
    mv nextflow /usr/local/bin/nextflow && chmod +x /usr/local/bin/nextflow

RUN mkdir /epibuilder
COPY . /epibuilder
WORKDIR /epibuilder

RUN mvn clean install

RUN cp target/epibuilder-core-jar-with-dependencies.jar /epibuilder/epibuilder-core.jar

RUN rm -rf target/

RUN chmod +x /epibuilder/pipeline/epibuilder.sh
RUN ln -s /epibuilder/pipeline/epibuilder.sh /usr/local/bin/epibuilder

WORKDIR /
CMD ["epibuilder"]

ENTRYPOINT ["epibuilder"]
