{{- define "edusharing_rendering_service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_rendering_service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_rendering_service.fullname" -}}
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

{{- define "edusharing_rendering_service.labels" -}}
{{ include "edusharing_rendering_service.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_rendering_service.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_rendering_service.labels.instance" -}}
{{ include "edusharing_rendering_service.labels.app" . }}
{{ include "edusharing_rendering_service.labels.version" . }}
{{- end -}}

{{- define "edusharing_rendering_service.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "edusharing_rendering_service.labels.app" -}}
app: {{ include "edusharing_rendering_service.fullname" . }}
app.kubernetes.io/name: {{ include "edusharing_rendering_service.fullname" . }}
{{- end -}}

{{- define "edusharing_rendering_service.pvc.share" -}}
share-{{ include "edusharing_rendering_service.fullname" . }}
{{- end -}}

{{- define "edusharing_rendering_service.edusharing_rediscluster" -}}
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

{{- define "edusharing_rendering_service.edusharing_rendering_mariadb" -}}
{{- if .Values.edusharing_rendering_mariadb.fullnameOverride -}}
{{- .Values.edusharing_rendering_mariadb.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "edusharing-rendering-mariadb" .Values.edusharing_rendering_mariadb.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "edusharing_rendering_service.edusharing_repository_service" -}}
{{- if .Values.edusharing_repository_service.fullnameOverride -}}
{{- .Values.edusharing_repository_service.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "edusharing-repository-service" .Values.edusharing_repository_service.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}
