{{- define "edusharing_repository_service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository_service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository_service.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "edusharing_repository_service.labels" -}}
{{ include "edusharing_repository_service.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_repository_service.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_repository_service.labels.instance" -}}
{{ include "edusharing_repository_service.labels.app" . }}
{{ include "edusharing_repository_service.labels.version" . }}
{{- end -}}

{{- define "edusharing_repository_service.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "edusharing_repository_service.labels.app" -}}
app: {{ include "edusharing_repository_service.fullname" . }}
app.kubernetes.io/name: {{ include "edusharing_repository_service.fullname" . }}
{{- end -}}

{{- define "edusharing_repository_service.pvc.share" -}}
share-{{ include "edusharing_repository_service.fullname" . }}
{{- end -}}

{{- define "edusharing_repository_service.edusharing_rediscluster" -}}
{{- if .Values.edusharing_rediscluster.fullnameOverride -}}
{{- .Values.edusharing_rediscluster.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "edusharing-rediscluster" .Values.edusharing_rediscluster.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "edusharing_repository_service.edusharing_repository_postgresql" -}}
{{- if .Values.edusharing_repository_postgresql.fullnameOverride -}}
{{- .Values.edusharing_repository_postgresql.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "edusharing-repository-postgresql" .Values.edusharing_repository_postgresql.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "edusharing_repository_service.edusharing_repository_search_elastic" -}}
{{- if .Values.edusharing_repository_search_elastic.fullnameOverride -}}
{{- .Values.edusharing_repository_search_elastic.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "edusharing-repository-search-elastic" .Values.edusharing_repository_search_elastic.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "edusharing_repository_service.edusharing_repository_search_solr4" -}}
{{- if .Values.edusharing_repository_search_solr4.fullnameOverride -}}
{{- .Values.edusharing_repository_search_solr4.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "edusharing-repository-search-solr4" .Values.edusharing_repository_search_solr4.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "edusharing_repository_service.edusharing_repository_transform" -}}
{{- if .Values.edusharing_repository_transform.fullnameOverride -}}
{{- .Values.edusharing_repository_transform.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "edusharing-repository-transform" .Values.edusharing_repository_transform.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}
