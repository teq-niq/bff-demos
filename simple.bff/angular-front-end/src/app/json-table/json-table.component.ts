import { Component, Input } from '@angular/core';

@Component({
  selector: 'json-table',
  standalone: true,
  templateUrl: './json-table.component.html',
  styleUrls: ['./json-table.component.css']
})
export class JsonTableComponent {

  @Input() data: any;

  isObject(value: any): boolean {
    return value && typeof value === 'object' && !Array.isArray(value);
  }

  isArray(value: any): boolean {
    return Array.isArray(value);
  }

  objectKeys(obj: any): string[] {
    return Object.keys(obj || {});
  }
}
