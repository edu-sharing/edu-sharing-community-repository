import { CdkConnectedOverlay, ConnectedPosition } from '@angular/cdk/overlay';
import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { Subject } from 'rxjs';
import { Filter, SearchFieldService } from './search-field.service';

@Component({
    selector: 'app-search-field',
    templateUrl: './search-field.component.html',
    styleUrls: ['./search-field.component.scss'],
})
export class SearchFieldComponent implements OnInit, OnDestroy {
    @Input() searchString: string;
    @Output() searchStringChange = new EventEmitter<string>();
    @Input() placeholder: string;
    @Output() search = new EventEmitter<string>();
    @Output() clear = new EventEmitter<void>();

    @ViewChild('input') input: ElementRef;
    @ViewChild(CdkConnectedOverlay) private overlay: CdkConnectedOverlay;
    @ViewChild('suggestionChip', { read: ElementRef })
    private firstSuggestionChip: ElementRef<HTMLElement>;

    readonly filters$ = this.searchField.filters$;
    readonly categories$ = this.searchField.categories$;
    readonly suggestions$ = this.searchField.suggestions$;
    showOverlay = false;
    hasSuggestions = true;
    readonly overlayPositions: ConnectedPosition[] = [
        {
            originX: 'center',
            originY: 'bottom',
            offsetX: 0,
            offsetY: 4,
            overlayX: 'center',
            overlayY: 'top',
        },
    ];

    private readonly destroyed$ = new Subject<void>();

    constructor(private searchField: SearchFieldService) {}

    ngOnInit(): void {}

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    onSubmit(): void {
        this.showOverlay = false;
        this.search.emit(this.searchString);
    }

    onClear(): void {
        this.searchString = '';
        this.searchStringChange.emit('');
        this.clear.emit();
    }

    inputHasFocus(): boolean {
        return document.activeElement === this.input?.nativeElement;
    }

    onAddFilter(property: string, filter: Filter): void {
        this.searchField.addFilter(property, filter);
    }

    onRemoveFilter(property: string, filter: Filter): void {
        this.searchField.removeFilter(property, filter);
    }

    onOutsideClick(event: MouseEvent): void {
        const clickTarget = event.target as HTMLElement;
        if (!(this.overlay.origin.elementRef.nativeElement as HTMLElement).contains(clickTarget)) {
            this.showOverlay = false;
        }
    }

    focusOverlayIfOpen(event: Event): void {
        if (this.firstSuggestionChip) {
            this.firstSuggestionChip.nativeElement.focus();
            event.stopPropagation();
            event.preventDefault();
        }
    }

    onDetach(): void {
        const focusWasOnOverlay = this.overlay.overlayRef.overlayElement.contains(
            document.activeElement,
        );
        if (focusWasOnOverlay) {
            this.input.nativeElement.focus();
        }
        this.showOverlay = false;
    }

    onInputBlur(event: FocusEvent): void {
        if (!this.overlay.overlayRef.overlayElement.contains(event.relatedTarget as HTMLElement)) {
            this.showOverlay = false;
        }
    }
}
