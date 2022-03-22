{{- define "edusharing_rendering_service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_rendering_service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
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
app: {{ include "edusharing_rendering_service.name" . }}
app.kubernetes.io/name: {{ include "edusharing_rendering_service.name" . }}
{{- end -}}

{{- define "edusharing_rendering_service.pvc.share.config" -}}
share-config-{{ include "edusharing_rendering_service.name" . }}
{{- end -}}

{{- define "edusharing_rendering_service.pvc.share.data" -}}
share-data-{{ include "edusharing_rendering_service.name" . }}
{{- end -}}
